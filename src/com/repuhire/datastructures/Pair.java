package com.repuhire.datastructures;

import java.io.Serializable;

import com.google.common.base.Objects;

/***
 * Data structure which wraps two objects.
 */
public class Pair<A, B> implements Serializable {

	private static final long serialVersionUID = -7899408930176161494L;
	private A a;
	private B b;

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	public A getFirst() {
		return a;
	}

	public B getSecond() {
		return b;
	}

	public void setFirst(A a) {
		this.a = a;
	}

	public void setSecond(B b) {
		this.b = b;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(a, b);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Pair) {
			Pair other = (Pair) obj;
			Object otherFirst = other.getFirst();
			Object otherSecond = other.getSecond();

			return Objects.equal(a, otherFirst) && Objects.equal(b, otherSecond);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append(a);
		sb.append(", ");
		sb.append(b);
		sb.append(">");
		return sb.toString();
	}

	public static <C, D> Pair<C, D> of(C first, D second) {
		return new Pair<C, D>(first, second);
	}

}
