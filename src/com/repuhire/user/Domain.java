package com.repuhire.user;

import com.google.common.base.Objects;

/***
 * Represents one domain to search within.
 * E.g. a university is a domain.
 */
public class Domain {

	//Each domain is modeled as a string
	//representing its identity
	private final String domainIdentifier;

	public Domain(String domainIdentifier) {
		this.domainIdentifier = domainIdentifier;
	}

	//-----
	//Getters
	//------
	public String getDomainIdentifier() {
		return domainIdentifier;
	}

	//------
	//Overrides
	//------
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Domain) {
			Domain other = (Domain) obj;
			return Objects.equal(other, obj);
		} else {
			return false;
		}
	};

	@Override
	public int hashCode() {
		return Objects.hashCode(domainIdentifier);
	}

	@Override
	public String toString() {
		return "Domain Id: " + domainIdentifier;
	}

}
