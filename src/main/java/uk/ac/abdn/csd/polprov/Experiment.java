package uk.ac.abdn.csd.polprov;

import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.JenaUtil;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class Experiment 
{
    public static void main( String[] args )
    {
    	
    	// the following is based largely on the examples in the SPIN API
        System.out.println( "Running..." );
        System.out.println(System.getProperty("user.dir"));

		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
        
        // load in the policy, scenario and rule files
        Model model = ModelFactory.createDefaultModel(); 
        model.read("file:rdf/hypertensionScenario.rdf");
        model.read("file:rdf/hypertensionPolicy.rdf");
        model.read("file:rdf/smart.owl");
        
        // Create OntModel with imports
     	OntModel ontModel = JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM, model);

		// Create and add Model for inferred triples
     	Model policyTriples = ModelFactory.createDefaultModel();
     	Model possibleTriples = ModelFactory.createDefaultModel();
     	ontModel.addSubModel(policyTriples);
     	ontModel.addSubModel(possibleTriples);

		// Register locally defined functions
		SPINModuleRegistry.get().registerAll(ontModel, null);

		// Run all inferences
		SPINInferences.run(ontModel, policyTriples, null, null, false, null);
		System.out.println("Inferred policy enactment triples: " + policyTriples.size());
        model.read("file:rdf/policies.rdf");
		SPINInferences.run(ontModel, possibleTriples, null, null, false, null);
		System.out.println("Possible PROV triples inferred: " + possibleTriples.size());

        StmtIterator st = possibleTriples.listStatements();
        while(st.hasNext()) {
        	System.out.println(st.next().toString());
        }


        
        // generate the power set of 'degradation situations'
        
        // for each one run the rules and see how many inferences we keep
        // for now, no breakdown by rule type
        
        
    }
}
