package com.drkwitht.resource;

import java.util.TreeSet;

/**
 * This class encapsulates data and logic for generating response bodies given a URI describing a "route" to a resource.
 */
public class StaticResponder {
    private TreeSet<String> routeCollection;  // URI mapped to resource object
    private StaticResource routeResource;

    public StaticResponder(String[] routes, StaticResource resource) throws Exception {
        if (routes == null)
            throw new Exception("Expected route strings, found null.");
        
        if (resource == null)
            throw new Exception("Expected resource object, found null.");

        routeCollection = new TreeSet<String>();
        routeResource = resource;    
        
        for (String routeString : routes) {
            routeCollection.add(routeString);
        }
    }

    public boolean hasRoute(String route) {
        return routeCollection.contains(route);
    }

    public StaticResource yieldResource() {
        return routeResource;
    }
}
