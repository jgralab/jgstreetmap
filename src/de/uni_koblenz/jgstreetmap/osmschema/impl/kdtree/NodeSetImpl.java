/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package de.uni_koblenz.jgstreetmap.osmschema.impl.kdtree;

import de.uni_koblenz.jgralab.impl.IncidenceIterable;
import de.uni_koblenz.jgralab.impl.VertexImpl;

import de.uni_koblenz.jgralab.AttributedElement;
import de.uni_koblenz.jgralab.Edge;
import de.uni_koblenz.jgralab.EdgeDirection;
import de.uni_koblenz.jgralab.Graph;
import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.GraphIOException;

import de.uni_koblenz.jgstreetmap.osmschema.Node;
import de.uni_koblenz.jgstreetmap.osmschema.OsmSchema;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class NodeSetImpl extends VertexImpl implements de.uni_koblenz.jgralab.Vertex, de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet {

	protected LinkedList<Node> nodeSet;
	
	public NodeSetImpl(int id, Graph g) {
		super(id, g, OsmSchema.instance().vc_kdtree_NodeSet);
	}

	public LinkedList<Node> getNodeSet(){
		return nodeSet;
	}
	
	public void setNodeSet(LinkedList<Node> set){
		nodeSet=set;
	}
	
	public void add2NodeSet(Node n){
		nodeSet.add(n);
	}
	
	public java.lang.Class<? extends AttributedElement> getM1Class() {
		return de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet.class;
	}

	public Object getAttribute(String attributeName) throws NoSuchFieldException {
		throw new NoSuchFieldException("kdtree.NodeSet doesn't contain an attribute " + attributeName);
	}

	@SuppressWarnings("unchecked")
	public void setAttribute(String attributeName, Object data) throws NoSuchFieldException {
		throw new NoSuchFieldException("kdtree.NodeSet doesn't contain an attribute " + attributeName);
	}

	public void readAttributeValues(GraphIO io) throws GraphIOException {
	}

	public void writeAttributeValues(GraphIO io) throws GraphIOException, IOException {
	}

	/* add all valid from edges */
	private static Set<java.lang.Class<? extends Edge>> validFromEdges = new HashSet<java.lang.Class<? extends Edge>>();
	
	/* (non-Javadoc)
	 * @see jgralab.Vertex:isValidAlpha()
	 */
	public boolean isValidAlpha(Edge edge) {
		return validFromEdges.contains(edge.getClass());
	}
	
	{

		validFromEdges.add(de.uni_koblenz.jgstreetmap.osmschema.impl.HasElementImpl.class);

	}
	
	/* add all valid to edges */
	private static Set<java.lang.Class<? extends Edge>> validToEdges = new HashSet<java.lang.Class<? extends Edge>>();
	
	/* (non-Javadoc)
	 * @see jgralab.Vertex:isValidOemga()
	 */
	public boolean isValidOmega(Edge edge) {
		return validToEdges.contains(edge.getClass());
	}
	
	{

		validToEdges.add(de.uni_koblenz.jgstreetmap.osmschema.impl.kdtree.HasSetImpl.class);

	}

	public de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet getNextNodeSet() {
		return (de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet)getNextVertexOfClass(de.uni_koblenz.jgstreetmap.osmschema.kdtree.NodeSet.class);
	}

	public de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet getFirstHasSet() {
		return (de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet)getFirstEdgeOfClass(de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet.class);
	}

	public de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet getFirstHasSet(EdgeDirection orientation) {
		return (de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet)getFirstEdgeOfClass(de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet.class, orientation);
	}

	public de.uni_koblenz.jgstreetmap.osmschema.HasElement getFirstHasElement() {
		return (de.uni_koblenz.jgstreetmap.osmschema.HasElement)getFirstEdgeOfClass(de.uni_koblenz.jgstreetmap.osmschema.HasElement.class);
	}

	public de.uni_koblenz.jgstreetmap.osmschema.HasElement getFirstHasElement(EdgeDirection orientation) {
		return (de.uni_koblenz.jgstreetmap.osmschema.HasElement)getFirstEdgeOfClass(de.uni_koblenz.jgstreetmap.osmschema.HasElement.class, orientation);
	}
	

	private static java.util.HashSet<Class<? extends Edge>> connectedElementEdgeSet = new java.util.HashSet<Class<? extends Edge>>();
	{
		connectedElementEdgeSet.add(de.uni_koblenz.jgstreetmap.osmschema.HasElement.class);
	}

	public java.util.List<? extends de.uni_koblenz.jgstreetmap.osmschema.Node> getElementList() {
		java.util.List<de.uni_koblenz.jgstreetmap.osmschema.Node> list = new java.util.ArrayList<de.uni_koblenz.jgstreetmap.osmschema.Node>();
		de.uni_koblenz.jgstreetmap.osmschema.HasElement edge = getFirstHasElement(EdgeDirection.OUT);
		while (edge != null) {
			if (connectedElementEdgeSet.contains(edge.getM1Class())) {
				list.add((de.uni_koblenz.jgstreetmap.osmschema.Node)edge.getThat());
			}
			edge = edge.getNextHasElement(EdgeDirection.OUT);
		}
		return list;
	}

	public de.uni_koblenz.jgstreetmap.osmschema.HasElement addElement(de.uni_koblenz.jgstreetmap.osmschema.Node vertex) {
		return ((de.uni_koblenz.jgstreetmap.osmschema.OsmGraph)getGraph()).createHasElement(this, vertex);
	}

	public void removeElement(de.uni_koblenz.jgstreetmap.osmschema.Node vertex) {
	    Edge e = getFirstHasElement();
	    while (e != null && e.getThat() == vertex) {
	        e.delete();
	        e = getFirstHasElement();
	    }
	    while (e != null) {
	        Edge f = e.getNextEdge();
	        while (f != null && f.getThat() == vertex) {
	           f.delete();
	           f = e.getNextEdge();
	        }
	        e = f;
	   }
	}
	

	private static java.util.HashSet<Class<? extends Edge>> connectedKeyEdgeSet = new java.util.HashSet<Class<? extends Edge>>();
	{
		connectedKeyEdgeSet.add(de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet.class);
	}

	public java.util.List<? extends de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key> getKeyList() {
		java.util.List<de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key> list = new java.util.ArrayList<de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key>();
		de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet edge = getFirstHasSet(EdgeDirection.IN);
		while (edge != null) {
			if (connectedKeyEdgeSet.contains(edge.getM1Class())) {
				list.add((de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key)edge.getThat());
			}
			edge = edge.getNextHasSet(EdgeDirection.IN);
		}
		return list;
	}

	public de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet addKey(de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key vertex) {
		return ((de.uni_koblenz.jgstreetmap.osmschema.OsmGraph)getGraph()).createHasSet(vertex, this);
	}

	public void removeKey(de.uni_koblenz.jgstreetmap.osmschema.kdtree.Key vertex) {
	    Edge e = getFirstHasSet();
	    while (e != null && e.getThat() == vertex) {
	        e.delete();
	        e = getFirstHasSet();
	    }
	    while (e != null) {
	        Edge f = e.getNextEdge();
	        while (f != null && f.getThat() == vertex) {
	           f.delete();
	           f = e.getNextEdge();
	        }
	        e = f;
	   }
	}

	public Iterable<de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet> getHasSetIncidences() {
		return new IncidenceIterable<de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet>(this, de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet.class);
	}
	
	
	public Iterable<de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet> getHasSetIncidences(EdgeDirection direction) {
		return new IncidenceIterable<de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet>(this, de.uni_koblenz.jgstreetmap.osmschema.kdtree.HasSet.class, direction);
	}

	public Iterable<de.uni_koblenz.jgstreetmap.osmschema.HasElement> getHasElementIncidences() {
		return new IncidenceIterable<de.uni_koblenz.jgstreetmap.osmschema.HasElement>(this, de.uni_koblenz.jgstreetmap.osmschema.HasElement.class);
	}
	
	
	public Iterable<de.uni_koblenz.jgstreetmap.osmschema.HasElement> getHasElementIncidences(EdgeDirection direction) {
		return new IncidenceIterable<de.uni_koblenz.jgstreetmap.osmschema.HasElement>(this, de.uni_koblenz.jgstreetmap.osmschema.HasElement.class, direction);
	}

}
