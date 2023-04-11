package query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mindswap.pellet.tableau.blocking.SubsetBlocking;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class ConjunctiveQuery {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((answerVars == null) ? 0 : answerVars.hashCode());
		result = prime * result + ((atoms == null) ? 0 : atoms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConjunctiveQuery other = (ConjunctiveQuery) obj;
		if (answerVars == null) {
			if (other.answerVars != null)
				return false;
		} else if (!answerVars.equals(other.answerVars))
			return false;
		if (atoms == null) {
			if (other.atoms != null)
				return false;
		} else if (!atoms.equals(other.atoms))
			return false;
		return true;
	}

	private List<String> answerVars;
	
	private List<QueryAtom> atoms;

	public ConjunctiveQuery() {
		answerVars = new ArrayList<>();
		atoms = new ArrayList<>();
	}

	public List<String> getAnswerVars() {
		return answerVars;
	}

	public void setAnswerVars(List<String> answerVars) {
		this.answerVars = answerVars;
	}

	public ConjunctiveQuery(ConjunctiveQuery cq) {
		answerVars = new ArrayList<>();
		answerVars.addAll(cq.answerVars);
		atoms = new ArrayList<>();
		for (QueryAtom s : cq.atoms)
		{
			QueryAtom newAtom = new QueryAtom(s);
			atoms.add(newAtom);
		}
	}

	public List<QueryAtom> getAtoms() {
		return atoms;
	}

	public void setAtoms(List<QueryAtom> atoms) {
		this.atoms = atoms;
	}

	public void addAtom(QueryAtom qa) {
		atoms.add(qa);
		
	}

	@Override
	public String toString() {
		return "ConjunctiveQuery [atoms=" + atoms + "]";
	}

	public Set<ConjunctiveQuery> tBoxRewrite(OntModel infModel) {
		Set <ConjunctiveQuery> fullRew = new HashSet<>(); 
		fullRew.add(this);
		List <ConjunctiveQuery> rewriten = new ArrayList<>();
		
		
		for (int i=0; i<fullRew.size();i++)
		{
			ConjunctiveQuery cq = (ConjunctiveQuery) fullRew.toArray()[i];
			if (!rewriten.contains(cq))
			{
				List <ConjunctiveQuery> rews = getOneStepRews (cq,infModel);
				fullRew.addAll(rews);
			
				
				rewriten.add(cq);
				Set<ConjunctiveQuery>cqs = reduce(cq);
				fullRew.addAll(cqs);
			}
		}
		return fullRew;
	}

	private Set<ConjunctiveQuery> reduce(ConjunctiveQuery cq) {
		
		Set <ConjunctiveQuery> newCQs = new HashSet<>();
		for (QueryAtom atom1 : cq.getAtoms())
		{
			if (atom1.isUnary())
			{
				for (QueryAtom atom2 : cq.getAtoms())
				{
					if (atom2.isUnary() && !atom2.equals(atom1)  && atom2.getConcept().toString().equals(atom1.getConcept().toString()))
					{
						String var1 = atom1.getTerms().keySet().toArray()[0].toString();
						String var2 = atom2.getTerms().keySet().toArray()[0].toString();
						
						if ((var2.startsWith("?") && !var1.equals("_")) || var2.equals("_"))
						{
							// replace var2 by var1
							ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
							newCQ.atoms.remove(atom2);
							replace(newCQ, var2, var1);
							newCQs.add(newCQ);
						}
						
					}
				}
			}
			else {
				for (QueryAtom atom2 : cq.getAtoms())
				{
					
					
					if (!atom2.equals(atom1) && atom2.isBinary() && atom2.getRole().getURI().equals( atom1.getRole().getURI()))
					{
						//System.out.println("----- ");
					
						String var11 = (String) atom1.getTerms().keySet().toArray()[0];
						String var21 = (String) atom2.getTerms().keySet().toArray()[0];
						
						String var12 = (String) atom1.getTerms().values().toArray()[0];
						String var22 = (String) atom2.getTerms().values().toArray()[0];
						
						if (!var21.equals(var11) && ((var21.startsWith("?") && !var11.equals("_")) || var21.equals("_")))
						{
							// replace var2 by var1
							ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
							newCQ.atoms.remove(atom2);
							System.out.println("!!!! removed atom "+atom2);
							replace(newCQ, var21, var11);
							newCQs.add(newCQ);
							System.out.println("replace "+ var21+" by "+var11 +":"+newCQ);
							//System.out.println("----- ");
						}
						if (!var22.equals(var12) && ((var22.startsWith("?") && !var12.equals("_")) || var22.equals("_")))
						{
							// replace var2 by var1
							ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
							System.out.println("!!!! removed atom "+atom2);
							newCQ.atoms.remove(atom2);
							replace(newCQ, var22, var12);
							newCQs.add(newCQ);
							System.out.println("replace "+ var22+" by "+var12 +":"+newCQ);
							//System.out.println("----- ");
						}
						
					}
				}
				
			}
			
		}
		return newCQs;
	}

	public void replace(ConjunctiveQuery newCQ, String var1, String var2) {
		newCQ.getAtoms().forEach(at -> {
			if (at.isUnary() && at.getTerms().keySet().toArray()[0].equals(var1))
			{
				HashMap <String,String> newTerm = new HashMap<>();
				newTerm.put(var2, null);
				at.setTerms(newTerm);
			}
			if (at.isBinary())
			{
				String t1 = (String) at.getTerms().keySet().toArray()[0];
				String t2 = (String) at.getTerms().values().toArray()[0];
				HashMap <String, String> newTerm = new HashMap<>();
				if (t1.equals(var1) )
				{
					t1 = var2;
					
				}
				
				if (t2.equals(var1))
				{
					t2 = var2;
				}
				newTerm.put(t1, t2);
				at.setTerms(newTerm);
				
			}
		});
		if (newCQ.answerVars.contains(var1))
		{
			newCQ.answerVars.remove(var1);
			if (!var2.equals("_") && var2.startsWith("?"))
				newCQ.answerVars.add(var2);
		}
	}

	private boolean isApplicable(OntModel infModel, ConjunctiveQuery cq) {
		for (QueryAtom qa : cq.atoms)
		{
			if (qa.isUnary())
			{
				ExtendedIterator<OntClass> iter = infModel.getOntClass(qa.getConcept().toString()).listSubClasses();
				if (iter.toSet().size() > 1)
					return true;
				boolean  isRange = false;
				boolean isDomain = false;
				for (OntProperty prop : infModel.listAllOntProperties().toList())
				{
					if (prop.hasRange(qa.getConcept())){
						isRange = true;
					}
					if (prop.hasDomain(qa.getConcept()))
					{
						isDomain = true;
					}
				}
				if (isRange == true || isDomain == true)
				{
					return true;
				}
				
			}
			else 
			{
				// isBinary
				ExtendedIterator<? extends OntProperty> iter = infModel.getOntProperty(qa.getRole().toString()).listSubProperties();
				if (iter.toSet().size() > 1)
					return true;
				// check if object is non-join
				if (!isJoinVariable((String)qa.getTerms().values().toArray()[0],cq))
				{
					ExtendedIterator<OntClass> iter2 =  infModel.getSomeValuesFromRestriction((qa.getRole().toString())).listSubClasses();
					if (iter2.toSet().size() > 1)
					{
						return true;
					}
				}
				// check if subject is non-join
				if (!isJoinVariable((String)qa.getTerms().keySet().toArray()[0],cq))
				{
					OntProperty inverse = infModel.getOntProperty(qa.getRole().toString()).getInverse();
					ExtendedIterator<OntClass> iter2 =  infModel.getSomeValuesFromRestriction(inverse.toString()).listSubClasses();
					if (iter2.toSet().size() > 1)
					{
						return true;
					}
				}
				
			}
		}
		return false;
	}

	private boolean isJoinVariable(String object, ConjunctiveQuery cq) {
		int countOccurs = 0;
		for (QueryAtom qa: cq.getAtoms())
		{
			if (qa.getTerms().keySet().contains(object))
							countOccurs++;
			if (qa.getTerms().values().contains(object))
							countOccurs++;
		}
		if (countOccurs > 1 || cq.getAnswerVars().contains(object))
			return true;
		return false;
	}

	private List<ConjunctiveQuery> getOneStepRews(ConjunctiveQuery cq, OntModel infModel) {
		List<ConjunctiveQuery> rewr = new ArrayList<>();
		
		for (QueryAtom qa : cq.atoms)
		{
			if (qa.isUnary())
			{
				
				infModel.listClasses().toList().forEach(conc ->{
					
					if (conc.equals(qa.getConcept()))
					{
						
						if (conc.hasSubClass())
						{
						
							ExtendedIterator<OntClass> iter = infModel.getOntClass(qa.getConcept().toString()).listSubClasses();
							while (iter.hasNext()){
								OntClass subclass = iter.next();
								if (subclass.equals(infModel.getOntClass( OWL.Nothing.getURI() )))
									continue;
								ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
								newCQ.getAtoms().forEach(atom ->{
									
									if (atom.equals(qa))
									{
										
										if (!subclass.isRestriction())
										{
											atom.setConcept(subclass);
										}
										else{
											atom.setUnary(false);
											atom.setBinary(true);
											atom.setConcept(null);
											Restriction r = subclass.asRestriction();
											if (r.isSomeValuesFromRestriction())
											{
												SomeValuesFromRestriction smvR = r.asSomeValuesFromRestriction();
												atom.setRole(smvR.getOnProperty());
											}
											// USE _ for variables that are non-join and non-distinguishable
											HashMap<String, String> pairTerms= new HashMap<>();
											pairTerms.put((String) qa.getTerms().keySet().toArray()[0], "_");
											atom.setTerms(pairTerms);
										}
									}
								});
								rewr.add(newCQ);
							}
							
						}
						String val = (String) qa.getTerms().values().toArray()[0];
						if (!cq.isJoin(qa, val)  && (val == null || val.startsWith("?") || val.startsWith("_"))){
							infModel.listAllOntProperties().forEachRemaining(prop -> {
								//System.out.println("!!!! "+conc);
								if (prop.hasRange(conc.asResource()) && !prop.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomDataProperty"))
										&& !prop.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomObjectProperty"))){
									//System.out.println("CMON");
									ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
									newCQ.getAtoms().forEach(atom ->{

										if (atom.equals(qa))
										{
											//System.out.println("Binary Atom");
											// unary into binary atom
											atom.setUnary(false);
											atom.setBinary(true);
											atom.setConcept(null);
											atom.setRole(prop);
											HashMap<String, String> pairTerms = new HashMap<>();
											pairTerms.put("_", (String) qa.getTerms().keySet().toArray()[0]);
											atom.setTerms(pairTerms);
										}
									});
									rewr.add(newCQ);
								}
								if (prop.hasDomain(conc.asResource()) && !prop.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomDataProperty"))
										&& !prop.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomObjectProperty")) )
								{
									ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
									newCQ.getAtoms().forEach(atom ->{

										if (atom.equals(qa))
										{
											// unary into binary atom
											atom.setUnary(false);
											atom.setBinary(true);
											atom.setRole(prop);
											atom.setConcept(null);
											HashMap<String, String> pairTerms = new HashMap<>();
											pairTerms.put( (String) qa.getTerms().keySet().toArray()[0],"_");
											atom.setTerms(pairTerms);
										}
									});
									rewr.add(newCQ);
								}
							});
						}
					}
				});
				
				
			}
			else 
			{
				// isBinary
				ExtendedIterator<? extends OntProperty> iter = infModel.getOntProperty(qa.getRole().toString()).listSubProperties();
				//if (iter.toSet().size() > 1)
				{
					while (iter.hasNext())
					{
						
						OntProperty subProp = iter.next();
						if (subProp.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomDataProperty")) || 
								subProp.equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomObjectProperty"))	)
							continue;
						ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
						newCQ.getAtoms().forEach(atom -> {
							if (atom.equals(qa))
							{
								atom.setRole(subProp);
							}
						});
						rewr.add(newCQ);
					}
					
				}
				
				// check for inverses 
				OntProperty inv = infModel.getOntProperty(qa.getRole().toString()).getInverse();
				if (inv!=null){
					ConjunctiveQuery newCQ = new ConjunctiveQuery(cq);
					newCQ.getAtoms().forEach(atom -> {
						if (atom.equals(qa))
						{
							atom.setRole(inv);
							// switch direction of terms
							HashMap<String, String> newPair = new HashMap<>();
							newPair.put((String)atom.getTerms().values().toArray()[0], (String)atom.getTerms().keySet().toArray()[0]);
							atom.setTerms(newPair);
						}
					});
					rewr.add(newCQ);
					}
				
				
				// check if object is non-join
				//if ()
				{
					//System.out.println(qa);
					//System.out.println(qa.isBinary());
					
					infModel.listRestrictions().forEachRemaining(restriction -> {
						if (restriction.getOnProperty().equals(qa.getRole()))
						{
							String val = (String)qa.getTerms().values().toArray()[0];
							if (!isJoinVariable(val,cq)  && (val==null || val.startsWith("?") || val.startsWith("_"))) {
							OntClass subclass = restriction.getSubClass();
							//System.out.println("*****"+subclass);
							ConjunctiveQuery newCQ2 = new ConjunctiveQuery(cq);
							newCQ2.getAtoms().forEach(atom -> {
								if (atom.equals(qa))
								{
									if (subclass.isRestriction())
									{
										Restriction r = subclass.asRestriction();
										if (r.isSomeValuesFromRestriction() &&
												!r.asSomeValuesFromRestriction().getOnProperty().equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomDataProperty"))
												&& !r.asSomeValuesFromRestriction().getOnProperty().equals(infModel.getOntProperty("http://www.w3.org/2002/07/owl#bottomObjectProperty")) )
										{
											SomeValuesFromRestriction smvR = r.asSomeValuesFromRestriction();
											atom.setRole(smvR.getOnProperty());
										}
										// USE _ for variables that are non-join and non-distinguishable
										HashMap<String, String> pairTerms= new HashMap<>();
										pairTerms.put((String) qa.getTerms().keySet().toArray()[0], "_");
										atom.setTerms(pairTerms);
									}
									else{
										atom.setUnary(true);
										atom.setBinary(false);
										atom.setRole(null);
										atom.setConcept(subclass);
										
										// TODO USE "" or null ?
										HashMap<String, String> pairTerms= new HashMap<>();
										pairTerms.put((String) qa.getTerms().keySet().toArray()[0], "");
										atom.setTerms(pairTerms);
									}
									
								}
							});
							rewr.add(newCQ2);
							}
						}
						else {
							String key= (String)qa.getTerms().keySet().toArray()[0];
							if (!isJoinVariable(key,cq) && (key.startsWith("?") || key.startsWith("_"))){
							OntProperty inverse = infModel.getOntProperty(qa.getRole().toString()).getInverse();
							
							if (restriction.getOnProperty().equals(inverse)){
								OntClass subclass = restriction.getSubClass();
							
								//System.out.println("*****"+subclass);
								ConjunctiveQuery newCQ2 = new ConjunctiveQuery(cq);
								newCQ2.getAtoms().forEach(atom -> {
									if (atom.equals(qa))
									{
										if (subclass.isRestriction())
										{
											Restriction r = subclass.asRestriction();
											if (r.isSomeValuesFromRestriction())
											{
												SomeValuesFromRestriction smvR = r.asSomeValuesFromRestriction();
												atom.setRole(smvR.getOnProperty());
											}
											// USE _ for variables that are non-join and non-distinguishable
											HashMap<String, String> pairTerms= new HashMap<>();
											pairTerms.put((String) qa.getTerms().values().toArray()[0], "_");
											atom.setTerms(pairTerms);
										}
										else{
											atom.setUnary(true);
											atom.setBinary(false);
											atom.setRole(null);
											atom.setConcept(subclass);
											
											// TODO USE "" or null ?
											HashMap<String, String> pairTerms= new HashMap<>();
											pairTerms.put((String) qa.getTerms().values().toArray()[0], "");
											atom.setTerms(pairTerms);
										}
									}
								});
								rewr.add(newCQ2);
								
							}
						}
						}
					});
					//ExtendedIterator<OntClass> iter2 =  infModel.getSomeValuesFromRestriction((qa.getRole().toString())).listSubClasses();
				//while (iter2.hasNext())
				//	{
				//		OntClass subclass = iter2.next();
						
				//	}
				//}
				// check if subject is non-join
				//if ()
				//{
				//	OntProperty inverse = infModel.getOntProperty(qa.getRole().toString()).getInverse();
					
					//ExtendedIterator<OntClass> iter2 =  infModel.getSomeValuesFromRestriction(inverse.toString()).listSubClasses();
					//while (iter2.hasNext())
					//{
				//		OntClass subclass = iter2.next();
						
					//}
				}
				
			}
		}
		return rewr;
	}
	
	public Set<ConjunctiveQuery> assumptionsRewriting (ConjunctiveQuery assump)
	{
		Set <ConjunctiveQuery> rewr = new HashSet<>();
		int n= this.atoms.size();
		for (int i = 0; i < (1<<n); i++) 
        { 
           
			ConjunctiveQuery gamma = new ConjunctiveQuery();
            // Print current subset 
            for (int j = 0; j < n; j++) {
  
                // (1<<j) is a number with jth bit 1 
                // so when we 'and' them with the 
                // subset number we get which numbers 
                // are present in the subset and which 
                // are not 
                if ((i & (1 << j)) > 0) {
                    gamma.addAtom(this.atoms.get(j));
                }
            }
           // System.out.println("----------");
           // System.out.println("Gamma: "+gamma.toString());
            Set<HashMap<String, String>> homomorphisms = getHomomorphism (gamma,assump);
           // System.out.println(" Homomorphisms: ");
            for (HashMap<String,String> h : homomorphisms) {
            if (h!=null)
            {
            	
            	// drop gamma from this.atoms
            	ConjunctiveQuery cq = new ConjunctiveQuery(this);
            	//System.out.println(" ORIG Q  "+cq);
            	for (QueryAtom qa : gamma.getAtoms())
            	{
            		cq.atoms.remove(qa);
            	}
            	
            	// add equality atoms
            	for (String var : toKeep(gamma)) {
            		
            		HashMap<String, String> terms = new HashMap<>();
            		terms.put(var,h.get(var));
            		QueryAtom eqAtom = new QueryAtom(true, false, null, null, terms);
            		// add answer vars
            		if (!cq.answerVars.contains(h.get(var)))
            			cq.answerVars.add(h.get(var));
            		cq.addAtom(eqAtom);
            	}
            	
            	for (String var : assump.answerVars)
            	{
            		if (h.values().contains(var))
            		{
            			HashMap<String, String> terms = new HashMap<>();
            			terms.put(var,var);
            			QueryAtom eqAtom = new QueryAtom(true, false, null, null, terms);
            			// add answer vars
            			if (!cq.answerVars.contains(var))
            				cq.answerVars.add(var);
            			if (!cq.atoms.contains(eqAtom)) {
            						cq.addAtom(eqAtom);
            			}
            			
            		}
            	}
            	// add answer variables
            	//System.out.println(" HOMOMORPHISM "+h);
            	//System.out.println("***** HOM VALS "+h.values());
            	
            	//System.out.println("rewr ansVars: "+ cq.answerVars);
            	//System.out.println(" rewr wrt h: "+cq);
            //	System.out.println("---------------------");
            	rewr.add(cq);
            }
        }
        }
		return rewr;
		
	}
	

	public Set<String> getVars()
	{
		Set <String> setVars = new HashSet<>();
		
		atoms.forEach(atom -> {
			HashMap<String,String> terms = atom.getTerms();
			setVars.add((String) terms.keySet().toArray()[0]);
			if (atom.isBinary())
				setVars.add((String)terms.values().toArray()[0]);
		});
		return setVars;
	}

	
	private Set <HashMap<String,String>> generateMappings(Set<String> gammaVars, Set<String> assumpVars)
	{
		
			
			Set <HashMap<String, String>> resultSet = new HashSet<>();
			
				
			
			for (String varGamma : gammaVars)
			{
				for (String varAssump : assumpVars)
				{
					// create new set without varGamma
					Set <String> subset = new HashSet<>();
					for (String s : gammaVars){
						if (!s.equals(varGamma))
							subset.add(s);
					}
							//gammaVars.stream().filter(v -> !v.equals(varGamma)).collect(Collectors.toSet());
					Set <HashMap<String, String>> combos  = generateMappings(subset, assumpVars);
					if (combos.isEmpty())
					{
						HashMap<String, String> h = new HashMap<>();
						h.put(varGamma, varAssump);
						resultSet.add(h);
					}
					else{
						for (HashMap<String, String> map : combos){
							map.put(varGamma, varAssump);
							//combos.add(map);
							
							}
						resultSet.addAll(combos);
						}
					
				}
			}
			return resultSet;
			
	}
	
	
	
	
	private Set<HashMap<String, String>> getHomomorphism(ConjunctiveQuery gamma, ConjunctiveQuery assump) {
		
		Set <HashMap<String, String>> homomorphisms = new HashSet<>();
		
		// GUESS: generate all mappings
		//System.out.println("VarsGamma "+gamma.getVars());
		//System.out.println("VarsAssump "+assump.getVars());
		Set<HashMap<String, String>> mappings = generateMappings(gamma.getVars(), assump.getVars());
		//System.out.println("ALL Mappings: ");
		//System.out.println("  "+mappings);
		// CHECK: filter them out
		for (HashMap<String, String> map :  mappings) {
			
			// check if it is a homomorphism
			
			boolean isHom = true;
			for (QueryAtom qa : gamma.atoms)
			{
				boolean hasMatch = false;
				if (qa.isUnary())
				{
					String var = (String) qa.getTerms().keySet().toArray()[0];
					for (QueryAtom atom : assump.atoms) {
						if (atom.isUnary() && 
								atom.getConcept().toString().equals(qa.getConcept().toString()) && 
								   atom.getTerms().keySet().toArray()[0].equals(map.get(var)))
						{
							hasMatch = true;
						}
					}
				}
				else { // it's binary
					String var1 = (String) qa.getTerms().keySet().toArray()[0];
					String var2 = (String) qa.getTerms().values().toArray()[0];
					for (QueryAtom atom : assump.atoms){
						if (atom.isBinary() && atom.getRole().toString().equals(qa.getRole().toString()) 
								&& atom.getTerms().keySet().toArray()[0].equals(map.get(var1)) 
								&& atom.getTerms().values().toArray()[0].equals(map.get(var2))){
							hasMatch = true;
						}
					}
				}
				if (!hasMatch){
					isHom = false;
					break;
				}
			}
			if (!isHom)
			{
				continue;
			}
			
			// check if keep variables are mapped to free variables
			boolean keepConstraint = true;
			for (String var : toKeep(gamma))
			{
				if (!assump.answerVars.contains(map.get(var)))
				{
					keepConstraint = false;
					break;
				}
			}
			if (!keepConstraint)
			{
				continue;
			}
			
			// check if shared variables are mapped to themselves 
			boolean strongHom = true;
			for (String var : assump.getVars())
			{
				//System.out.println("*** "+var+" "+map.get(var));
				
				if (gamma.getVars().contains(var)){
					//System.out.println("#### " +map.get(var));
					// check if it is the identity on map
					if (!map.get(var).equals(var))
					{
						
						strongHom =  false;
						
					}
				}
			}
			//System.out.println("*** Strong hom "+strongHom);
			if (!strongHom)
			{
				continue;
			}
			
			homomorphisms.add(map);
				
		}
		return homomorphisms;
	}

	private Set<String> toKeep(ConjunctiveQuery gamma) {
		Set <String> keepVars = new HashSet<>();
		for (String varInGamma : gamma.getVars())
		{
			if (this.answerVars.contains(varInGamma))
			{
				keepVars.add(varInGamma);
			}
			else{
				this.atoms.forEach(atom -> {
					if (!gamma.atoms.contains(atom))
					{
						if (atom.isBinary())
						{
							if (atom.getTerms().containsKey(varInGamma) || atom.getTerms().containsValue(varInGamma))
							{
								keepVars.add(varInGamma);
							}
						}
						else{
							if (atom.getTerms().containsKey(varInGamma))
							{
								keepVars.add(varInGamma);
							}
						}
					}
				});
			}
		}
		
		return keepVars;
	}

	public boolean isJoin(QueryAtom atom, String subj) {
		boolean isJoin = false;
		for (QueryAtom at : this.atoms )
		{
			if (!at.equals(atom) && ((at.isUnary() && at.getConcept()!=null) || at.isBinary()))
			{
				if (at.getTerms().containsKey(subj) || (at.isBinary() && at.getTerms().containsValue(subj)))
					isJoin = true;
				
			}
		}
			
		return isJoin;
	}
	
	

}
