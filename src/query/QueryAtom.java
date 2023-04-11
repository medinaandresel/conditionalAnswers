package query;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;

public class QueryAtom {

	private boolean isUnary ;
	private boolean isBinary;
	
	// null if binary
	private OntClass concept;
	
	// null if unary
	private Property role; 
	
	// key is the subject, value is the object (if binary)
	private HashMap<String, String> terms;

	
	public QueryAtom(boolean isUnary, boolean  isBinary,OntClass  concept, Property role,  String... terms)
	{
		this.isUnary = isUnary;
		this.isBinary = isBinary;
		this.concept = concept;
		this.role = role;
		this.terms = new HashMap<>();
		if (terms.length==1) {
			
			
				this.terms.put(terms[0], null);
			
		}
		else {
			this.terms.put(terms[0], terms[1]);
			
		}
	}


	public QueryAtom(QueryAtom s) {
		this.isUnary = s.isUnary;
		this.isBinary = s.isBinary;
		this.concept = s.concept;
		this.role = s.role;
		this.terms = new HashMap<>();
		for (String key : s.terms.keySet())
		{
			this.terms.put(key, s.terms.get(key));
		}
	}





	public QueryAtom(boolean isUnary, boolean isBinary, OntClass concept, Property role,
			HashMap<String, String> terms) {
		super();
		this.isUnary = isUnary;
		this.isBinary = isBinary;
		this.concept = concept;
		this.role = role;
		this.terms = terms;
	}


	public boolean isUnary() {
		return isUnary;
	}


	


	@Override
	public String toString() {
		return "QueryAtom [isUnary=" + isUnary + ", isBinary=" + isBinary + ", concept=" + concept + ", role=" + role
				+ ", terms=" + terms + "]";
	}


	public boolean isBinary() {
		return isBinary;
	}


	public OntClass getConcept() {
		return concept;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((concept == null) ? 0 : concept.hashCode());
		result = prime * result + (isBinary ? 1231 : 1237);
		result = prime * result + (isUnary ? 1231 : 1237);
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((terms == null) ? 0 : terms.hashCode());
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
		QueryAtom other = (QueryAtom) obj;
		if (concept == null) {
			if (other.concept != null)
				return false;
		} else if (concept!=null && other.concept!=null && !concept.toString().equals(other.concept.toString()))
			return false;
		if (isBinary != other.isBinary)
			return false;
		if (isUnary != other.isUnary)
			return false;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (role!=null && other.role!=null && !role.toString().equals(other.role.toString()))
			return false;
		if (terms == null) {
			if (other.terms != null)
				return false;
		} else if (!terms.equals(other.terms))
			return false;
		return true;
	}


	public void setUnary(boolean isUnary) {
		this.isUnary = isUnary;
	}


	public void setBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}


	public void setConcept(OntClass concept) {
		this.concept = concept;
	}


	public void setRole(Property role) {
		this.role = role;
	}


	public void setTerms(HashMap<String, String> terms) {
		this.terms = terms;
	}


	public Property getRole() {
		return role;
	}


	public HashMap<String, String> getTerms() {
		return terms;
	}


	public boolean isGround() {
		
		String key = (String) terms.keySet().toArray()[0];
		String val = terms.get(key);
		if (key.startsWith("?"))
			return false;
		if ((this.isBinary && val==null) || val.startsWith("?"))
			return false;
		return true;
	}
	
	
}
