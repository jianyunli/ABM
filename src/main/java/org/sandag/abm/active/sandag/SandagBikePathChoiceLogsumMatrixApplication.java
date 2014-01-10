package org.sandag.abm.active.sandag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import java.text.DecimalFormat;

public class SandagBikePathChoiceLogsumMatrixApplication extends AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{

    private static final String[] MARKET_SEGMENT_NAMES = {"Logsum"};
    private static final int[] MARKET_SEGMENT_GENDER_VALUES = {1};
    private static final int[] MARKET_SEGMENT_TOUR_PURPOSE_INDICES = {1};
    private static final boolean[] MARKET_SEGMENT_INBOUND_TRIP_VALUES = {false};
    
    private static final double MINUTES_PER_MILE = 6.0;
    
    private ThreadLocal<SandagBikePathChoiceModel> model;
    private Person[] persons;
    private Tour[] tours;
    
    private static final String DEBUG_ORIGIN = "active.debug.origin";
    private static final String DEBUG_DESTINATION = "active.debug.destination";
    
    public SandagBikePathChoiceLogsumMatrixApplication(PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration, 
    		                                           final Map<String,String> propertyMap)
    {
        super(configuration);
        model = new ThreadLocal<SandagBikePathChoiceModel>() {
        	@Override
        	protected SandagBikePathChoiceModel initialValue() {
        		return new SandagBikePathChoiceModel((HashMap<String,String>) propertyMap);
        	}
        };
        persons = new Person[MARKET_SEGMENT_NAMES.length];
        tours = new Tour[MARKET_SEGMENT_NAMES.length];
        
        //for dummy person
        SandagModelStructure modelStructure = new SandagModelStructure();
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            persons[i] = new Person(null,1,modelStructure);
            persons[i].setPersGender(MARKET_SEGMENT_GENDER_VALUES[i]);
            tours[i] = new Tour(persons[i],1,MARKET_SEGMENT_TOUR_PURPOSE_INDICES[i]);
        }
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        SandagBikePathAlternatives alts = new SandagBikePathAlternatives(alternativeList);
        double[] logsums = new double[MARKET_SEGMENT_NAMES.length + 1]; 
        
        boolean debug = ( alternativeList.getODPair().getFromNode().getId() == Integer.parseInt(this.propertyMap.get(DEBUG_ORIGIN)) )
                           && ( alternativeList.getODPair().getToNode().getId() == Integer.parseInt(this.propertyMap.get(DEBUG_DESTINATION)) );
        
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            logsums[i] = model.get().getPathLogsums(persons[i], alts, MARKET_SEGMENT_INBOUND_TRIP_VALUES[i], tours[i], debug);
        }
        double[] probs = model.get().getPathProbabilities(persons[0], alts, false, tours[0], debug);
        double avgDist = 0;
        for (int i=0; i<alts.getPathCount(); i++) {
            avgDist += probs[i] * alts.getDistanceAlt(i);
        }
        logsums[logsums.length-1] = avgDist * MINUTES_PER_MILE;
        return logsums;    
    }
    
    public static void main(String ... args) {
        String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        Map<String,String> propertyMap = new HashMap<String,String>();
        SandagBikeNetworkFactory factory;
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
        List<PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>> configurations = new ArrayList<>();
        SandagBikePathChoiceLogsumMatrixApplication application;
        
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();

        configurations.add(new SandagBikeTazPathAlternativeListGenerationConfiguration(propertyMap, network));
        configurations.add(new SandagBikeMgraPathAlternativeListGenerationConfiguration(propertyMap, network));
        String[] fileProperties = new String[] {"active.logsum.matrix.file.bike.taz", "active.logsum.matrix.file.bike.mgra"};
        
        DecimalFormat formatter = new DecimalFormat("#.###");
        

        
        
        
        for(int i=0; i<configurations.size(); i++)  {
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration  = configurations.get(i);
            String filename = configuration.getOutputDirectory() + "/" + propertyMap.get(fileProperties[i]);
            application = new SandagBikePathChoiceLogsumMatrixApplication(configuration,propertyMap);
            
            new File(configuration.getOutputDirectory()).mkdirs();
            
            Map<NodePair<SandagBikeNode>,double[]> logsums = application.calculateMarketSegmentLogsums();
            Map<Integer,Integer> originCentroids = configuration.getInverseOriginZonalCentroidIdMap();
            Map<Integer,Integer> destinationCentroids = configuration.getInverseDestinationZonalCentroidIdMap();
            
            try
            {
                FileWriter writer = new FileWriter(new File(filename));
                writer.write("i, j, " + Arrays.toString(MARKET_SEGMENT_NAMES).substring(1).replaceFirst("]", "") + ", time" + "\n");
                for (NodePair<SandagBikeNode> od : logsums.keySet()) {
                    double[] values = logsums.get(od); 
                    writer.write(originCentroids.get(od.getFromNode().getId()) + ", " + destinationCentroids.get(od.getToNode().getId()));
                    for (double v : values) {
                        writer.write(", " + formatter.format(v));
                    }
                    writer.write("\n");
                }
                writer.flush();
                writer.close();  
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            
            
        }
        
        try
        {
            System.out.println("Writing edges with derived attributes to " + configurations.get(0).getOutputDirectory() + "edgeResults.csv");
            FileWriter writer =  new FileWriter(configurations.get(0).getOutputDirectory() + "edgeResults.csv");
            writer.write("fromNode,toNode,bikeClass,lanes,functionalClass,centroidConnector,autosPermitted,cycleTrack,bikeBlvd,distance,gain,bikeCost,walkCost\n");
            Iterator<SandagBikeEdge> eit = network.edgeIterator();
            while (eit.hasNext()) {
                SandagBikeEdge edge = eit.next();
                writer.write(edge.getFromNode().getId() +",");
                writer.write(edge.getToNode().getId() +",");
                writer.write(edge.bikeClass +",");
                writer.write(edge.lanes +",");
                writer.write(edge.functionalClass +",");
                writer.write(edge.centroidConnector +",");
                writer.write(edge.autosPermitted +",");
                writer.write(edge.cycleTrack +",");
                writer.write(edge.bikeBlvd +",");
                writer.write(edge.distance +",");
                writer.write(edge.gain +",");
                writer.write(edge.bikeCost +",");
                writer.write(edge.walkCost +"\n");
            }
            writer.flush();
            writer.close();
            
            System.out.println("Writing nodes with derived attributes to " + configurations.get(0).getOutputDirectory() + "nodeResults.csv");
            writer =  new FileWriter(configurations.get(0).getOutputDirectory() + "nodeResults.csv");
            writer.write("id,x,y,mgra,taz,tap,signalized,centroid\n");
            Iterator<SandagBikeNode> nit = network.nodeIterator();
            while (nit.hasNext()) {
                SandagBikeNode node = nit.next();
                writer.write(node.getId() +",");
                writer.write(node.x +",");
                writer.write(node.y +",");
                writer.write(node.mgra +",");
                writer.write(node.taz +",");
                writer.write(node.tap +",");
                writer.write(node.signalized +",");
                writer.write(node.centroid +"\n");
            }
            writer.flush();
            writer.close();
            
            System.out.println("Writing traversals with derived attributes to " + configurations.get(0).getOutputDirectory() + "traversalResults.csv");
            writer =  new FileWriter(configurations.get(0).getOutputDirectory() + "traversalResults.csv");
            writer.write("start,thru,end,turnType,bikecost,thruCentroid,signalExclRight,unlfrma,unlfrmi,unxma,unxmi\n");
            Iterator<SandagBikeTraversal> tit = network.traversalIterator();
            while (tit.hasNext()) {
                SandagBikeTraversal t = tit.next();
                writer.write(t.getFromEdge().getFromNode().getId() +",");
                writer.write(t.getFromEdge().getToNode().getId() +",");
                writer.write(t.getToEdge().getToNode().getId() +",");
                writer.write(t.turnType.getKey() +",");
                writer.write(t.cost +",");
                writer.write(t.thruCentroid +",");
                writer.write(t.signalExclRightAndThruJunction +",");
                writer.write(t.unsigLeftFromMajorArt +",");
                writer.write(t.unsigLeftFromMinorArt +",");
                writer.write(t.unsigCrossMajorArt +",");
                writer.write(t.unsigCrossMinorArt +"\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}
