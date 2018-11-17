package org.commitment_issues.delivery_agents;

/*
 * This class implements the an edge of a directed graph to be used in 
 * the Dijkstra algorithm.
 * 
 * Courtesy of Lars Vogel (c) 2009, 2016 vogella GmbH. Version 1.2, 29.09.2016
 * Source: http://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 */

public class Edge  {
    private final String id;
    private final Vertex source;
    private final Vertex destination;
    private final float weight;
    
    public Edge(String id, Vertex source, Vertex destination, float weight) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }
    public Vertex getDestination() {
        return destination;
    }

    public Vertex getSource() {
        return source;
    }
    
    public float getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return source + " " + destination;
    }


}
