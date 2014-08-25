package uk.ac.abdn.csd.polprov;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Experiment 
{
	
	public static final String POLICY_NAMESPACE = "http://www.dotrural.ac.uk/ontologies/policies#";
	public static final String PROV_NAMESPACE = "http://www.w3.org/ns/prov#";
	
    public static void main( String[] args )
    {
    	
    	// the following is based largely on the examples in the SPIN API
        System.out.println( "Starting up..." );
        System.out.println(System.getProperty("user.dir"));

		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
        
        // load in the policy, scenario and rule files
		System.out.println("Loading models...");
        Model model = ModelFactory.createDefaultModel(); 
        model.read("file:rdf/hypertensionScenario.rdf");
        
        // proceed to read the domain specific policy rules, policyProv language 
        // and inference rules
        model.read("file:rdf/hypertensionPolicy.rdf");
        //model.read("file:rdf/smart.owl");
        
        // strip out provenance links not required for evaluation
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#atLocation"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#hadRole"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#generated"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#used"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#atTime"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#qualifiedAssociation"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#qualifiedGeneration"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#qualifiedUsage"), (RDFNode)null));
        model.remove(model.listStatements(null, model.getProperty("http://www.w3.org/ns/prov#qualifiedStart"), (RDFNode)null));
        
        // Create OntModel with imports
     	OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, model);

		// Create and add models for inferred triples
     	 // inferred by simulated policy enactment rules
     	Model policyTriples = ModelFactory.createDefaultModel();
     	// inferred by policy-prov rules	
     	Model possibleTriples = ModelFactory.createDefaultModel(); 
     	ontModel.addSubModel(policyTriples);
     	ontModel.addSubModel(possibleTriples);

		// Register locally defined functions
		SPINModuleRegistry.get().registerAll(ontModel, null);

		// Run policy inferences
		System.out.println("Running inference rules...");
		SPINInferences.run(ontModel, policyTriples, null, null, false, null);
        Set<Statement> policySet = filterStatementsByNS(policyTriples.listStatements(), POLICY_NAMESPACE);
		System.out.println("Inferred policy enactment triples: " + policySet.size());
		
		// Run missing provenance inference rules
        model.read("file:rdf/policies.rdf");
		SPINInferences.run(ontModel, possibleTriples, null, null, false, null);
		// remove and reset in preparation for the experiment
		ontModel.removeSubModel(possibleTriples);
		ontModel.reset();

		// Seperate out pol inferences and prov statements into sets
		Set<Statement> provSet = filterStatementsByNS(model.listStatements(), PROV_NAMESPACE);
        System.out.println("PROV statements found: " + provSet.size());
        Set<Statement> inferenceSet = filterStatementsByNS(possibleTriples.listStatements(), POLICY_NAMESPACE);
		System.out.println("Possible PROV triples inferred: " + inferenceSet.size());

        // generate the power set of 'degradation situations'
        System.out.print("Generating power set of provenance degradations...");
        Set<Set<Statement>> pset = Sets.powerSet(provSet);
        System.out.println("finished " + pset.size() + " situations.");
        
        // for each one run the rules and see how many inferences we keep
        // for now, no breakdown by rule type
        int i = 0;
        
        // result storage should be an array of arrays where the main
        // index is the number of missing elements, and the array inside
        // holds the inference counts 
        Map<Integer,List<Integer>> results = new HashMap<Integer, List<Integer>>();
        // create an empty list for each number of possible missing links
        for(int j = 0; j <= provSet.size(); j++) results.put(j, new ArrayList<Integer>());
        
        for(Set<Statement> s : pset) {
        	// remove the links in s temporarily
        	List<Statement> testList = Lists.newArrayList(s); 
        	ontModel.remove(testList);
        	// regenerate the inferences
         	Model testModel = ModelFactory.createDefaultModel();
         	ontModel.addSubModel(testModel);
    		SPINInferences.run(ontModel, testModel, null, null, false, null);
        	System.out.print("\r"+ i++ +" of "+pset.size());
        	// add to result structure
        	// we subtract three to quickly get rid of the three rdf:type inferences that seem to get made
        	results.get(s.size()).add((int) testModel.size()-3);
        	// replace removed statements
    		ontModel.add(testList);
    		// remove inferences and reset
    		ontModel.removeSubModel(testModel);
    		ontModel.reset();
        }
        
        // print results
        System.out.println("\n\nRESULTS");
        System.out.println("=======");
        System.out.println("Missing links\tAvg. Inferred statements");
        for(int k = 1; k < possibleTriples.size(); k++)
        {
        	double sum = 0;
        	for(int r : results.get(k)) sum += r;
        	double avg = sum / results.get(k).size();
        	System.out.println(k + "\t\t" + avg);
        }
        
    }
    
    private static Set<Statement> filterStatementsByNS(StmtIterator si, String ns)
    {
        Set<Statement> set = new HashSet<Statement>();
    	while(si.hasNext()) {
        	Statement s = si.next();
        	if(s.getPredicate().getNameSpace().equals(ns)) {
        		set.add(s);
        		System.out.println(s.toString());
        	}
        }
    	return set;
    }
    
}
