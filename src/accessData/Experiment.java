package accessData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.NetworkChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.syntax.ElementWalker;
import com.hp.hpl.jena.sparql.util.Timer;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import query.ConjunctiveQuery;
import query.QueryAtom;

public class Experiment {
	
	

	public static OntModel getOntologyModel(String ontoFile)
	{   
		
	    OntModel ontoModel = ModelFactory.createOntologyModel();
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
	
	
	@SuppressWarnings("resource")
	private static String readQuery(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		  return new String(encoded, StandardCharsets.UTF_8);
	}

	
	private static long testFedQuery(String path, Model localdata, long timeout)
	{
		String fedstring = null;
		long timetoexec = 0;
		try {
			fedstring = readQuery(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Query fedquery = QueryFactory.create(fedstring);
		QueryExecution qexec = null;
		try{
			qexec = QueryExecutionFactory.create(fedstring, localdata);
			qexec.setTimeout(timeout);
			Timer t1 = new Timer();
			t1.startTimer();
			ResultSet results = qexec.execSelect() ;
			ResultSetFormatter.consume(results);
			long x = t1.endTimer();
			timetoexec = x;
			System.out.println("Returned answers: "+results.getRowNumber());

		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally{
			qexec.close();
		}
		return timetoexec;
	}
	
	private static Set<ConjunctiveQuery> testQueryTBoxRewriting (String path, OntModel ont)
	{
		String queryString = null;
		try {
			queryString = readQuery(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Query query = QueryFactory.create(queryString);
		ConjunctiveQuery cq = transformInCQ(query, ont);
		Set<ConjunctiveQuery> cqRew = cq.tBoxRewrite(ont);
		System.out.println("TBox rewriting size: "+cqRew.size());
		for (ConjunctiveQuery q : cqRew)
			System.out.println(q);
		return cqRew;
	}
	
private static Query toSparql(ConjunctiveQuery cq) {
		
		//System.out.println(cq);
		String select = "SELECT ";
		if (cq.getAnswerVars().isEmpty())
			select = select +" *";
		else {
			for (String var : cq.getAnswerVars())
			{
				select += " "+var;
			}
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
					
					// check if they appear somewhere else in the query otherwise just drop them
					
					
					
					if (!obj.startsWith("?"))
						obj = "<"+obj+">";
					if (cq.isJoin(atom, subj) || cq.isJoin(atom, obj)){
						if (filter.equals("FILTER ("))
							filter = filter+ " "+subj +" = "+obj;
						else
							filter = filter+ " && "+ subj +" = "+obj;
					}
					
					//if (cq.getAnswerVars().contains(subj) && !cq.isJoin(atom, subj))
					//	bgps = bgps + subj+ " <"+ RDF.type +"> "+"<"+OWL.Thing+">" + " . \n";
					
					//if (cq.getAnswerVars().contains(obj) && !cq.isJoin(atom, obj))
					//	bgps = bgps + obj+ " <"+ RDF.type +"> "+"<"+OWL.Thing+">" + " . \n";
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
		//System.out.println(select+where+bgps+"}");
		if (filter.equals("FILTER (")) 
			return QueryFactory.create(select+where+bgps+"}");
		else
			return QueryFactory.create(select+where+bgps+" . \n "+filter+" ) }");
	}
	
	private static void testAssumRewriting(String assumpPath, Set<ConjunctiveQuery> tboxRew, OntModel ontology,
			Model localData, long timeout) {
		
		String assumpString = null;
		try {
			assumpString = readQuery(assumpPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Query assump = QueryFactory.create(assumpString);
		ConjunctiveQuery assumpCQ = transformInCQ(assump, ontology);
		
		Set <ConjunctiveQuery> fullRew = new HashSet<>();
		for (ConjunctiveQuery q : tboxRew)
		{
			//System.out.println("FOR "+q);
			//System.out.println("ASSUMP REWR:");
			Set<ConjunctiveQuery> rewr = q.assumptionsRewriting(assumpCQ);
			//System.out.println(rewr);
			//System.out.println("------");
			fullRew.addAll(rewr);
		}
		fullRew.addAll(tboxRew);
		System.out.println("FULL REW SIZE (incl. TBox rewr) "+ fullRew.size());
		
		long timeToEvalAll = 0;
		Set <ConjunctiveQuery> askQueries = new HashSet<>();
		Set <ConjunctiveQuery> selectQueries = new HashSet<>();
		long timeToConstructMinCans = 0;
		for (ConjunctiveQuery q: fullRew)
		{
				System.out.println(q.getAnswerVars());
				Query q2 = toSparql(q);
				System.out.println(q2);
				QueryExecution exec = QueryExecutionFactory.create(q2, localData);
				//exec.setTimeout(timeout);
				Timer t1 = new Timer();
				t1.startTimer();
				ResultSet results = exec.execSelect();
				
				//ResultSetFormatter.outputAsCSV(results);
				List<QuerySolution> ans = ResultSetFormatter.toList(results);
				timeToEvalAll += t1.endTimer();
				//System.out.println(" Eval query "+q2);
				// collect minCans 
				Timer t2 = new Timer();
				t2.startTimer();
				for (QuerySolution sol : ans)
				{
					//System.out.println("----- Sol "+sol.get("line"));
					ConjunctiveQuery groundCQ = new ConjunctiveQuery();
					ConjunctiveQuery nongroundCQ = new ConjunctiveQuery();
	
						
					for (QueryAtom atom : assumpCQ.getAtoms()){
						QueryAtom newAtom = new QueryAtom(atom);
						HashMap<String, String> terms = newAtom.getTerms();
						String key = (String)terms.keySet().toArray()[0];
						String val = terms.get(key);
						if (val!=null)
						{
							val = val.replace("?", "");
						}
						key = key.replace("?", "");
						//System.out.println("+++ "+key+" "+val);
						String newKey = "";
						String newVal = "";
						if (sol.getResource(key)!=null)
							newKey = sol.get(key).toString();
						else if (!key.contains("#")) {
							newKey = "?"+key;
						}else 
							newKey = key;
						if (val!= null && sol.getResource(val)!=null)
							newVal = sol.get(val).toString();
						else if (val != null && !val.contains("#")){
							newVal = "?"+val;
						}
						else newVal = val;
						
						HashMap<String,String> newterms = new HashMap<>();
						newterms.put(newKey, newVal);
						newAtom.setTerms(newterms);

						/*if (sol.getResource(key)!=null)
						{
							if (val!=null && sol.getResource(val)!=null)
							{
								//if (val.equals(var2))
								{

									terms.put(sol.get(key).toString(), sol.get(val).toString());

									terms.remove("?"+key);
									System.out.println(terms);
								}
							}
							else
							{

								terms.put(sol.get(key).toString(), "?"+val);

								terms.remove("?"+key);
								System.out.println("val "+terms);
							}
						}*/
						//System.out.println(newAtom);
						if (newAtom.isGround() || !assumpCQ.getAnswerVars().contains("?"+key) || !assumpCQ.getAnswerVars().contains("?"+val) ){
							//(!assumpCQ.getAnswerVars().contains("?"+key) && !assumpCQ.getAnswerVars().contains("?"+val) )){
							//System.out.println(newAtom);
							
							//if(groundCQ.isJoin(newAtom, subj))
							groundCQ.addAtom(newAtom);
						}
						else if ( assumpCQ.getAnswerVars().contains("?"+key) || assumpCQ.getAnswerVars().contains("?"+val) ){
							// x=x and x not in body but answer var in the rew
							// we mark this atom for select 
							nongroundCQ.addAtom(newAtom);
							
							
							//System.out.println(newAtom);
						}

					}
					
					
					//System.out.println("GR CQ "+ groundCQ.toString());
					askQueries.add(groundCQ);
					selectQueries.add(nongroundCQ);
				}
				
				timeToConstructMinCans += t2.endTimer();
				//System.out.println("------");
				
				
		}
		
		System.out.println("Time to eval all rewr (ms): "+timeToEvalAll);
		System.out.println("Time to compute min cond ans (ms):"+(timeToEvalAll+timeToConstructMinCans));
		
		System.out.println("Size of ask queries "+askQueries.size());
		System.out.println("Size of select queries "+selectQueries.size());
		
		
		testSparqlEndpoint(assump,askQueries, selectQueries, timeout); 
		
		
		//System.out.println("Size of set of min cond. ans.: "+askQueries.size());
		
	}
	
	
	
	
	private static void testSparqlEndpoint(Query assump, Set<ConjunctiveQuery> askQueries, Set <ConjunctiveQuery> selectQueries, long timeout) {
		
		long timetoevalall = 0;
		int condTrue =0;
		int condFalse = 0;
		for (ConjunctiveQuery c : askQueries)
		{
			if (c.getAtoms().isEmpty())
				continue;
			String queryAsk = toSparqlAsk(c);
			//System.out.println(queryAsk);
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:8080/fuseki/GeoData/query",
					queryAsk);
			qexec.setTimeout(timeout);
			Timer t = new Timer();
			t.startTimer();
			boolean ans = qexec.execAsk();
			timetoevalall +=t.endTimer();
			if (ans == true)
				condTrue++;
			else
				condFalse++;
		}
		int producedAns = 0;
		for (ConjunctiveQuery c : selectQueries)
		{
			if (c.getAtoms().isEmpty())
				continue;
			String querySelect = toSparql(c).toString();
			//System.out.println(querySelect);
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:8080/fuseki/GeoData/query",
					querySelect);
			qexec.setTimeout(timeout);
			
			Timer t = new Timer();
			t.startTimer();
			ResultSet results = qexec.execSelect();
			
			if (results.hasNext()){
				
				condTrue ++;
				producedAns ++;
			}
			//System.out.println("**** "+hasAns);
			//ResultSetFormatter.outputAsCSV(results);
			timetoevalall +=t.endTimer();		
		}
		System.out.println("Time to test all min. ground assumptions (ms): "+timetoevalall);
		System.out.println("Number of true assumtions "+condTrue);
		System.out.println("Number of false assumtions "+(condFalse+selectQueries.size()-producedAns));
		System.out.println("Size of min cond ans "+(askQueries.size()+selectQueries.size()-2));
	}

	private static String toSparqlAsk(ConjunctiveQuery cq) {
				//System.out.println("**** "+cq);
				String select = "ASK ";
				
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
						//System.out.println(atom.isBinary());
						String subj = atom.getTerms().keySet().toArray()[0].toString();
						String obj = (String) atom.getTerms().values().toArray()[0];
						//System.out.println(atom);
						if (subj == "_")  {
							subj = "?anon"+i;
							i++;
						}
						else if (!subj.startsWith("?"))
							subj = "<"+subj+">";
						if (obj == "_" || obj == null)  {
							obj = "?anon"+i;
							i++;
						}
						else if (!obj.startsWith("?"))
							obj = "<"+obj+">";
						
						bgps += subj +" <"+ atom.getRole() +"> "+ obj+" . \n";
						
					}
				}
				if (!bgps.isEmpty()) {
					int j = bgps.lastIndexOf(".");
					bgps = bgps.substring(0, j-1);
				}	
				//System.out.println(select+where+bgps+" . \n "+filter+" ) }");
				if (filter.equals("FILTER (")) 
					return select+where+bgps+"}";
				else
					return select+where+bgps+" . \n "+filter+" ) }";
	}

	private static ResultSet computeMinCondAns(Set<ResultSet> allAnswSet) {
		
		return null;
	}

	public static void main(String[] args) {
		
		if (args.length != 4) {
			System.err.println("Usage: <FedQ> <query> <assumpQ> <timeout(ms)>");
			System.exit(1);
		}
		
		String fedPath= args[0];
		String qPath = args[1];
		String assumpPath = args[2];
		System.out.println(args[3]);
		long timeout = Long.parseLong(args[3]);
		
		
		// load local dataset
		
		Model localData  = ModelFactory.createDefaultModel();
		localData.read("data/vienna_rr_bm1/banks.owl");
		localData.read("data/vienna_rr_bm1/relHasBankOp.owl");
		localData.read("data/vienna_rr_bm1/shops.owl");
		localData.read("data/vienna_rr_bm1/streets.owl");
		localData.read("data/vienna_data.owl");
	//	localData.read("data/vienna_rr_bm1/relNext.owl");
		System.out.println("Local Data size: "+localData.size());
		
		// load ontology 
		OntModel ontology = getOntologyModel("ontology/GeoConceptsMyITS_KB_QL.owl");

		System.out.println("----------TEST FED QUERY");
		long timeFed = testFedQuery(fedPath, localData, timeout);
		System.out.println("Time to execute FedQuery(ms): "+timeFed);
		
		System.out.println("----------END TEST FED QUERY \n");
		
		
		
		System.out.println("---------- TEST QUERY TBox REWRITING");
		Set <ConjunctiveQuery> tboxRew = testQueryTBoxRewriting(qPath, ontology);	
		System.out.println("---------- Estimated time to obtain certain answers (ms): "+
				(tboxRew.size()*timeFed));
		System.out.println("---------- END TEST TBox REWRITING \n");
		
		System.out.println("---------- TEST ASSUMTIVE REWRITING");
		testAssumRewriting(assumpPath, tboxRew, ontology, localData, timeout);
		System.out.println("---------- END TEST ASSUMTIVE REWRITING");
		
		
	}

	

	

}
