package uk.ac.abdn.csd.polprov;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

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
        model.read("file:rdf/smart.owl");
        
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
		System.out.println("Inferred policy enactment triples: " + policyTriples.size());
		// Run missing provenance inference rules
        model.read("file:rdf/policies.rdf");
		SPINInferences.run(ontModel, possibleTriples, null, null, false, null);

		// Filter out pol inferences and prov statements into sets
        StmtIterator possibleStatements = possibleTriples.listStatements();
        StmtIterator allStatements = model.listStatements(); 
        Set<Statement> inferenceSet = new HashSet<Statement>();
        Set<Statement> provSet = new HashSet<Statement>();
        while(possibleStatements.hasNext()) {
        	Statement s = possibleStatements.next();
        	if(s.getPredicate().getNameSpace().equals(POLICY_NAMESPACE)) {
        		inferenceSet.add(s);
        		System.out.println(s.toString());
        	}
        }
		System.out.println("Possible PROV triples inferred: " + inferenceSet.size());

        while(allStatements.hasNext()) {
        	Statement s = allStatements.next();
        	if(s.getPredicate().getNameSpace().equals(PROV_NAMESPACE)) {
        		provSet.add(s);
        		System.out.println(s.toString());
        	}
        }
        System.out.println("PROV statements found: " + provSet.size());


        // generate the power set of 'degradation situations'
        System.out.print("Generating power set of provenance degradations...");
        Set<Set<Statement>> pset = Sets.powerSet(provSet);
        System.out.println("finished " + pset.size() + " situations.");
        
        // for each one run the rules and see how many inferences we keep
        // for now, no breakdown by rule type
        int i = 0;
        for(Set<Statement> s : pset)
        	System.out.print("\r"+i+++" of "+pset.size());
        
    }
}
