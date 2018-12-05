package org.commitment_issues.delivery_agents;

/*
 * This class implements a directed graph of nodes and edges to be used in 
 * the Dijkstra algorithm.
 * 
 * Courtesy of Lars Vogel (c) 2009, 2016 vogella GmbH. Version 1.2, 29.09.2016
 * Source: http://www.vogella.com/tutorials/JavaAlgorithmsDijkstra/article.html
 */

import java.util.List;

public class Graph {
    private final List<Vertex> vertexes;
    private final List<Edge> edges;

    public Graph(List<Vertex> vertexes, List<Edge> edges) {
        this.vertexes = vertexes;
        this.edges = edges;
    }

    public List<Vertex> getVertexes() {
        return vertexes;
    }

    public List<Edge> getEdges() {
        return edges;
    }



}