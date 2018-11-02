/**
 * 
 */
package de.evoila.cf.model;

import java.io.Serializable;

/**
 * @author Christian Brinker, evoila.
 *
 */
public interface BaseEntity<ID extends Serializable> {
	
	public ID getId();
}
