package io.pivotal.cdm.dto;

/**
 * Immutable pair for source and copy instance pairs.
 *
 */
public class InstancePair {

	private String sourceInstance;
	private String copyInstance;

	public InstancePair(String sourceInstance, String copyInstance) {
		this.sourceInstance = sourceInstance;
		this.copyInstance = copyInstance;
	}

	public String getSource() {
		return sourceInstance;
	}

	public String getCopy() {
		return copyInstance;
	}

	@Override
	public boolean equals(Object lhs) {
		return lhs instanceof InstancePair && this.hashCode() == lhs.hashCode();
	}

	@Override
	public int hashCode() {
		return sourceInstance.hashCode() + copyInstance.hashCode();
	}
}
