package accessData;

import org.apache.jena.riot.RDFDataMgr;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.DataRange;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.lang.SPARQLParser;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
import com.hp.hpl.jena.sparql.util.Timer;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import query.ConjunctiveQuery;
import query.QueryAtom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;




public class LoadHDT {
	
	public static void main(String[] args) throws Throwable {
		if (args.length != 2) {
			System.err.println("Usage: hdtsparql <hdt input> <SPARQL Query>.");
			System.exit(1);
		}

		//execSelectAndProcess(
		//		"http://localhost:8080/fuseki/GeoData/query",
		//		"SELECT ?subject ?predicate ?object WHERE {  ?subject ?predicate ?object } LIMIT 25");
		
		String fileHDT = args[0];
		String sparqlQuery ="";
		String sparqlRewriting ="";
		String assumpSparql = "";
		try{
			sparqlQuery = readQuery("queries/q1");
			//System.out.println(sparqlQuery);
			//sparqlRewriting = readQuery("queries/testq1");
			assumpSparql = readQuery("queries/hq1");
		}catch (IOException e)
		{
			e.printStackTrace();
		}

		
		// load Ontology 
		//OntModel inf = getOntologyModel("ontology/GeoConceptsMyITS_KB.owl");
				//OntModel base = ModelFactory.createOntologyModel();
		//base.read("ontology/GeoConceptsMyITS_KB.owl", null);
		
		
				
				
		// Reasoner reasoner = (Reasoner) PelletReasonerFactory.theInstance().create();
		OntModel base = getOntologyModel("ontology/GeoConceptsMyITS_KB_QL.owl");
		OntModel infModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, base);
		//infModel.getResource(uri)
		//OntClass djevent = infModel.getOntClass("http://www.kr.tuwien.ac.at/myits/geoconcepts/terms#_FeatureCode");
	//	infModel.listAllOntProperties().forEachRemaining(prop -> {
	//		System.out.println("prop "+prop.toString());
	//		System.out.println("    domain "+prop.getDomain());
	//		System.out.println("    range "+prop.getRange());
	//	});
		
		
		/*while (iter.hasNext())
		{
			DataRange cla = iter.next();
			if (cla.isRestriction()){
				Restriction r = cla.asRestriction();
				if (r.isSomeValuesFromRestriction())
				{
					SomeValuesFromRestriction smvR = r.asSomeValuesFromRestriction();
					System.out.println(smvR.getOnProperty().toString()+" "+smvR.getSomeValuesFrom());
				}
			}
			else{
				System.out.println(cla.toString());
			}
		}*/
	
		// Create HDT
		HDT hdt = HDTManager.mapIndexedHDT("queries/sparql.hdt", null);

		try {
			// Create Jena wrapper on top of HDT.
			HDTGraph graph = new HDTGraph(hdt);
			Model model = ModelFactory.createModelForGraph(graph);
			
			Model testModel  = ModelFactory.createDefaultModel();
			testModel.read("queries/vienna_data.owl");
			
			// ModelFactory.createUnion (m1,m2) -> to integrate multiple sources

			System.out.println("FEDERATED EXAMPLE");
			String fedstring = readQuery("queries/fq1");
			Query fedquery = QueryFactory.create(fedstring);
			System.out.println(fedquery);
			QueryExecution qexec = null;
			try{
				qexec = QueryExecutionFactory.create(fedstring, testModel);
				ResultSet results = qexec.execSelect() ;
				ResultSetFormatter.consume(results);
				//ResultSetFormatter.outputAsCSV(results);
				System.out.println("exec finished "+results.getRowNumber());
				
				
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}finally{
				qexec.close();
			}
			    
			System.out.println("END FED EXAMPLE");
			
			
			// Use Jena ARQ to execute the query.
			Query query = QueryFactory.create(sparqlQuery);
			ConjunctiveQuery cq = transformInCQ(query, base);
		
			
			System.out.println("CQ "+cq);
			
			Query hq = QueryFactory.create(assumpSparql);
			ConjunctiveQuery assump = transformInCQ(hq, base);
			System.out.println("H "+assump);
			Set<ConjunctiveQuery> cqRew = cq.tBoxRewrite(base);
			
			System.out.println("CQ TBox rew size: "+ cqRew.size());
			System.out.println("TBox Rewriting:");
			//for (ConjunctiveQuery cqR : cqRew)
			{
				//System.out.println(cqR);
				//System.out.println("-> SPARQL: ");
				//System.out.println(toSparql(cqR));
			}
			
			Set <ConjunctiveQuery> fullRew = new HashSet<>();
			for (ConjunctiveQuery q : cqRew)
			{
				//System.out.println("FOR "+q);
				//System.out.println("ASSUMP REWR:");
				Set<ConjunctiveQuery> rewr = q.assumptionsRewriting(assump);
				//System.out.println(rewr);
				//System.out.println("------");
				fullRew.addAll(rewr);
			}
			//fullRew.addAll(cqRew);
			System.out.println("FULL rew size: "+ fullRew.size());
			//System.out.println("FULL Rewriting: ");
			//for (ConjunctiveQuery q: fullRew)
			{
				//System.out.println(q);
			}
			
			Timer timer1 =new Timer();
			timer1.startTimer();
			for (ConjunctiveQuery q : cqRew){
				//System.out.println(q);
				Query q2 = toSparql(q);
				//System.out.println("-> SPARQL: ");
				//System.out.println(q2);
				QueryExecution exec = QueryExecutionFactory.create(q2, model);
				ResultSet results = exec.execSelect();
				ResultSetFormatter.consume(results) ;
				//System.out.println("Answ: ");
				//ResultSetFormatter.outputAsCSV(System.out, results);
			}
			long x = timer1.endTimer();
			System.out.println("Time to compute certain answers (ms) "+x);
			System.out.println("FULL REWR");
			
			
			Timer timer2= new Timer();
			timer2.startTimer();
			for (ConjunctiveQuery q : fullRew){
				//System.out.println(q);
				Query q2 = toSparql(q);
			//	System.out.println(q2);
				QueryExecution exec = QueryExecutionFactory.create(q2, model);
				ResultSet results = exec.execSelect();
				//System.out.println("Answ: ");
				ResultSetFormatter.consume(results);
			}
			long y = timer2.endTimer();
			System.out.println("Time to compute conditional answers (ms) "+y);
			
			//Query queryRewriting = QueryFactory.create(sparqlRewriting);
			//System.out.println("++++" + queryRewriting);
	
		
			
			//QueryExecution qeRew = QueryExecutionFactory.create(queryRewriting, model);
			
			try {
				// FIXME: Do ASK/DESCRIBE/CONSTRUCT 
				//Timer timer1 =new Timer();
				//timer1.startTimer();
				//ResultSet results = qe.execSelect();

				/*while(results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				System.out.println(sol.toString());
				}*/
				// Output query results	
				//ResultSetFormatter.consume(results) ;
				//long x = timer1.endTimer();
				//System.out.println("Query "+x);
				//ResultSetFormatter.outputAsCSV(System.out, results);
				
				//Timer timer2= new Timer();
				//timer2.startTimer();
				//ResultSet results2 = qeRew.execSelect();
				//results2.forEachRemaining(tuple -> {
					//String sparqlBoolQ = "PREFIX tuwt: <http://www.kr.tuwien.ac.at/myits/geoconcepts/terms#> \n" +
					//					 "ASK  { tuwt:1010572279 tuwt:isLocatedAlong ?u . \n " +
					//					 				 "<"+tuple.get("station").asResource().toString()+">"+ " tuwt:isLocatedAlong ?u }";
					//System.out.println(sparqlBoolQ);
					//Query boolQ = QueryFactory.create(sparqlBoolQ);
				
					//QueryExecution boolQe = QueryExecutionFactory.create(boolQ, model);
					//boolQe.execAsk(); 
				//});
				//ResultSetFormatter.consume(results2);
				//long y = timer2.endTimer();
				//System.out.println("Rew "+y);
				
			} finally {
			//	qe.close();				
			}
		} finally {			
			// Close
			hdt.close();
		}
	}

	private static Query toSparql(ConjunctiveQuery cq) {
		
		//System.out.println(cq);
		String select = "SELECT ";
		for (String var : cq.getAnswerVars())
		{
			select += " "+var;
		}
		String where = "\n WHERE { \n";
		String bgps = "";
		String filter = "FILTER (";
		int i = 0;
		for (QueryAtom atom : cq.getAtoms())
		{
			
			if (atom.isUnary())
			{
				
				if (atom.getConcept() != null){
					String t = atom.getTerms().keySet().toArray()[0].toString();
					if (!t.startsWith("?"))
					{
						t = "<"+t+">";
					}
					
					else if (t.equals("_"))
					{
						t = "?anon"+i;
						i++;
					}
					
					
					
					bgps = bgps + t+ " <"+ RDF.type +"> "+"<"+atom.getConcept()+">" + " . \n";
						
					
				}
				else
				{
					String subj = atom.getTerms().keySet().toArray()[0].toString();
					if (!subj.startsWith("?"))
						subj = "<"+subj+">";
					String obj = atom.getTerms().values().toArray()[0].toString();
					if (!obj.startsWith("?"))
						obj = "<"+obj+">";
					if (filter.equals("FILTER ("))
						filter = filter+ " "+subj +" = "+obj;
					else
						filter = filter+ " && "+ subj +" = "+obj;
				}
				
				
				
			}
			else {
				String subj = atom.getTerms().keySet().toArray()[0].toString();
				String obj = atom.getTerms().values().toArray()[0].toString();
				if (subj == "_")  {
					subj = "?anon"+i;
					i++;
				}
				else if (!subj.startsWith("?"))
					subj = "<"+subj+">";
				if (obj == "_")  {
					obj = "?anon"+i;
					i++;
				}
				else if (!obj.startsWith("?"))
					obj = "<"+obj+">";
				
				bgps += subj +" <"+ atom.getRole() +"> "+ obj+" . \n";
				
			}
		}
		int j = bgps.lastIndexOf(".");
		bgps = bgps.substring(0, j-1);
		//System.out.println(select+where+bgps+" . \n "+filter+" ) }");
		if (filter.equals("FILTER (")) 
			return QueryFactory.create(select+where+bgps+"}");
		else
			return QueryFactory.create(select+where+bgps+" . \n "+filter+" ) }");
	}

	

	public static OntModel getOntologyModel(String ontoFile)
	{   
		
	    OntModel ontoModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
	    InputStream in = null;
	    try 
	    {
	         in = FileManager.get().open(ontoFile);
	        try 
	        {
	            ontoModel.read(in, null);
	        } 
	        catch (Exception e) 
	        {
	            e.printStackTrace();
	        }
	        
	    } 
	    catch (JenaException je) 
	    {
	        System.err.println("ERROR" + je.getMessage());
	        je.printStackTrace();
	        System.exit(0);
	    }
	    finally{
	    	try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return ontoModel;
	}
	
	
	private static ConjunctiveQuery transformInCQ (Query query, OntModel infModel)
	{
		ConjunctiveQuery cq = new ConjunctiveQuery();
		final Set<Node> subjects = new HashSet<Node>();

		// This will walk through all parts of the query
		ElementWalker.walk(query.getQueryPattern(),
		    // For each element...
		    new ElementVisitorBase() {
		        // ...when it's a block of triples...
		        public void visit(ElementPathBlock el) {
		            // ...go through all the triples...
		            Iterator<TriplePath> triples = el.patternElts();
		            while (triples.hasNext()) {
		                // ...create atom
		                TriplePath triple = triples.next();
		                
		                if (triple.getPredicate().hasURI(RDF.type.toString()))
		                {
		                    
		                	boolean isUnary = true;
		                	boolean isBinary = false;
		                	Property role = null;
		                	OntClass concept = infModel.getOntClass(triple.getObject().getURI());
		                	String varName = triple.getSubject().toString();
		                	QueryAtom qa = new QueryAtom(isUnary, isBinary, concept, role, varName);
		                	cq.addAtom (qa);
		                }
		                else {
		                	if (triple.getPredicate().isURI())
		                	{
		                		boolean isUnary= false;
		                		boolean isBinary = true;
		                		Property role = infModel.getProperty(triple.getPredicate().getURI());
		                		OntClass concept = null;
		                		String subject = triple.getSubject().toString();
		                		String object = triple.getObject().toString();
		                		QueryAtom qa = new QueryAtom(isUnary, isBinary, concept, role, subject, object);
			                	cq.addAtom (qa);
		                	}
		                }
		            }
		        }
		    }
		);
		for (Var var : query.getProjectVars())
		{
			System.out.println(var.toString());
			cq.getAnswerVars().add(var.toString());
		}
		return cq;
	}
	
	public static void execSelectAndProcess(String serviceURI, String query) {
		QueryExecution q = QueryExecutionFactory.sparqlService(serviceURI,
				query);
		ResultSet results = q.execSelect();
		ResultSetFormatter.outputAsCSV(results);
	
	}
	
	@SuppressWarnings("resource")
	private static String readQuery(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		  return new String(encoded, StandardCharsets.UTF_8);
	}

}
