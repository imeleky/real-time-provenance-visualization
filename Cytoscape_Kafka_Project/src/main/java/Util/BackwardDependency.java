package Util;

import com.opencsv.CSVReader;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class BackwardDependency {

    private ArrayList<ArrayList<Integer>> stateCurrent;
    private ArrayList<ArrayList<Integer>> statePurge;
    private ArrayList<String> rowsCurrent;
    private ArrayList<String> columnsCurrent;
    private ArrayList<String> rowsPurge;
    private ArrayList<String> columnsPurge;
    private HashMap<String, String> varIdToNodeIdCurrent;
    private HashMap<String, String> varIdToNodeIdPurge;
    private Integer uniqueNodeId;

    public BackwardDependency (){
        stateCurrent            = new ArrayList<>();
        statePurge              = new ArrayList<>();
        varIdToNodeIdCurrent    = new HashMap<>();
        varIdToNodeIdPurge      = new HashMap<>();
        rowsCurrent             = new ArrayList<>();
        columnsCurrent          = new ArrayList<>();
        rowsPurge               = new ArrayList<>();
        columnsPurge            = new ArrayList<>();
        uniqueNodeId            = new Integer(0);
    }

    /*public void main(String[] args){
        currentColumnCount      = 1;
        currentRowCount         = 1;
        stateCurrent            = new Integer[currentRowCount][currentColumnCount];
        statePurge              = new Integer[currentRowCount][currentColumnCount];
        varIdToNodeIdCurrent    = new HashMap<>();
        varIdToNodeIdPurge      = new HashMap<>();
        rowsCurrent             = new ArrayList<>();
        columnsCurrent          = new ArrayList<>();
        rowsPurge               = new ArrayList<>();
        columnsPurge            = new ArrayList<>();

        try {
            Element node =  DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(("<prov:wasAssociatedWith>\n" +
                            "\t<prov:activity prov:ref=\"Process_33000\"/>\n" +
                            "\t<prov:agent prov:ref=\"Agent_001\"/>\n" +
                            "</prov:wasAssociatedWith>").getBytes()))
                    .getDocumentElement();

            updateState(node, "nodeId1", "nodeId2");
            printMatrix(stateCurrent);
            printMatrix(statePurge);

            node = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(("<prov:wasInformedBy>\n" +
                            "\t<prov:informed prov:ref=\"Process_237867\"/>\n" +
                            "\t<prov:informant prov:ref=\"Process_33000\"/>\n" +
                            "</prov:wasInformedBy>").getBytes())).getDocumentElement();
            updateState(node, "nodeId3", "nodeId4");

            printMatrix(stateCurrent);
            printMatrix(statePurge);
        }catch (Exception e){
            e.printStackTrace();
        }

    }*/

    public void printMatrix(ArrayList<ArrayList<Integer>> matrix){
        for(ArrayList<Integer> row : matrix){
            for(Integer cell : row){
                System.out.print(cell + " ");
            }
            System.out.print("\n");
        }

        System.out.println("--------------------------------------------------------------------------------------------------------------------");
    }

    // @param element: A prov-o notification (an edge)
    // @param node1Id: Node Id of source node
    // @param node2Id: Node Id of dest node

    public void updateState(String source, String destination){
        String sourceNode;
        String destNode;
        ArrayList<String> inputVars;
        String node1Id = (uniqueNodeId++).toString();
        String node2Id = (uniqueNodeId++).toString();

        // get source and destination nodes
        sourceNode   = source;
        destNode     = destination;

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "]\n" + "Initial State:");
        System.out.println("Current Rows: " + rowsCurrent.toString());
        System.out.println("Current Columns: " + columnsCurrent.toString());

        System.out.println("Purge Rows: " + rowsPurge.toString());
        System.out.println("Purge Columns: " + columnsPurge.toString());

        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);  //imy -->  printMatrix metodlarını perf ölçümleri sırasında // yapalım.

        System.out.println("Purge Matrix:");
        printMatrix(statePurge);   //imy -->  printMatrix metodlarını perf ölçümleri sırasında // yapalım.

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] New edge --> Source: " + sourceNode + " ->> Destination: " + destNode);
        // if incoming source node is a new value of a variable then cache current state and remove source node from current stat
        if(varIdToNodeIdCurrent.containsKey(sourceNode) && varIdToNodeIdCurrent.get(sourceNode) != node1Id){
            for(int i=0; i<rowsCurrent.size(); i++){
                if(rowsCurrent.get(i).equals(sourceNode)){
                    cacheDependencies(i);

                    rowsCurrent.remove(sourceNode);
                    stateCurrent.remove(i);

                    break;
                }
            }
        }

        // add new mapping sourceNode --> node1Id
        varIdToNodeIdCurrent.put(sourceNode, node1Id);

        // If source node is not in the matrix, add it to the matrix and also to rows list
        if(!rowsCurrent.contains(sourceNode)){
            rowsCurrent.add(sourceNode);
            stateCurrent.add(new ArrayList<Integer>());

            for(String temp : columnsCurrent){
                stateCurrent.get(stateCurrent.size()-1).add(new Integer(0));
            }
        }
        
        /*imy
        Erkan selam;
        
        StateCurrent ile matrix kulllanmak yerine StateCurrent<string yada integer, ArrayList<Integer>> kullanabilir miyiz performans ölçümü için.
        Bu şekilde, ust adımlarda yaptıgın gibi ilklendırırken for ile gezerek 0 lamana gerek kalmaz. Sadece iliskili olan nodeları bu nodeun
        hashmap te karşılık gelen listeye ekleyebiliriz. Bunu da purge ile boşaltmadan eski değerleri silmeden,  yeni dependency e ekleyebiliriz.
        
        Performans olarak en azından 2 for artıya gecerız, matrix için her iliski için olmasa da yer tutmayız , inputVars sıralı eklemesini daha yukarıda
        ki adımlara alabiliriz.
       
        ilk adım olarak buna bakabilir miyiz?
       
        */

        // If destination node is not in the matrix, add it to the matrix and also to columns list
        // After that, add new dependency sourceNode --> destNode
        if(!columnsCurrent.contains(destNode)){
            columnsCurrent.add(destNode);
            for(ArrayList<Integer> columns : stateCurrent){
                columns.add(new Integer(0));
            }
        }

        stateCurrent.get(rowsCurrent.indexOf(sourceNode)).set(columnsCurrent.indexOf(destNode), 1);

        // If source node equals to destNode then get backward provenance from state.purge, else get it from state.current
        if(sourceNode.equals(destNode)){
            inputVars = getBackwardProvenance(destNode, statePurge, rowsPurge, columnsPurge);
        }else {
            inputVars = getBackwardProvenance(destNode, stateCurrent, rowsCurrent, columnsCurrent);
        }

        // Update backward provenance of source node in state.current
        for(String var : inputVars){
            stateCurrent.get(rowsCurrent.indexOf(sourceNode)).set(columnsCurrent.indexOf(var), 1);
        }

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "]\n" + "Last State:");
        System.out.println("Current Rows: " + rowsCurrent.toString());
        System.out.println("Current Columns: " + columnsCurrent.toString());

        System.out.println("Purge Rows: " + rowsPurge.toString());
        System.out.println("Purge Columns: " + columnsPurge.toString());

        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);

        System.out.println("Purge Matrix:");
        printMatrix(statePurge); //imy -->  printMatrix metodlarını perf ölçümleri sırasında // yapalım.
    }

    // Bütün bir satır çekilemez mi??
    public ArrayList<String> getBackwardProvenance(String varId, ArrayList<ArrayList<Integer>> matrix, ArrayList<String> sourceList, ArrayList<String> columns){

        ArrayList<String> result    = new ArrayList<>();
        Integer varIndex            = sourceList.indexOf(varId);

        if(varIndex < 0){
            return result;
        }

        for(int i=0; i<columns.size(); i++){
            if(matrix.get(varIndex).get(i) == 1){
                result.add(columns.get(i));
            }
        }

        return result;
    }

    // Cache dependencies that removed from state.current
    public void cacheDependencies(Integer rowToCash){
        if(!rowsPurge.contains(rowsCurrent.get(rowToCash))){
            rowsPurge.add(rowsCurrent.get(rowToCash));
        }else {
            statePurge.remove(rowsPurge.indexOf(rowsCurrent.get(rowToCash)));
        }

        statePurge.add(stateCurrent.get(rowToCash));
        columnsPurge    = columnsCurrent;
    }

/*    // Remove a row from a matrix
    public Integer[][] removeRow(Integer i, Integer[][] matrix){
        for(int j=i; j<matrix.length-1; j++){
            matrix[j] = matrix[j+1];
        }

        currentRowCount--;
        return matrix;
    }*/

    // Find source node Id
    /*public String getSourceNodeId(Node element, String relationType){
        String result           = new String();
        String sourceNodeString = new String();

        switch (relationType){
            case "wasAssociatedWith":
                sourceNodeString = "activity";
                break;
            case "wasInformedBy":
                sourceNodeString = "informed";
                break;
            case "used":
                sourceNodeString = "activity";
                break;
            case "wasDerivedFrom":
                sourceNodeString = "generatedEntity";
                break;
            case "wasGeneratedBy":
                sourceNodeString = "entity";
                break;
        }

        for(int i=0; i<element.getChildNodes().getLength(); i++){
            if(element.getChildNodes().item(i).getNodeName().contains(sourceNodeString)){
                result = element.getChildNodes().item(i).getAttributes().getNamedItem("prov:ref").getNodeValue();
                break;
            }
        }

        return result;
    }*/

    // Find destination node Id
    /*public String getDestNodeId(Node element, String relationType){
        String result           = new String();
        String sourceNodeString = new String();

        switch (relationType){
            case "wasAssociatedWith":
                sourceNodeString = "agent";
                break;
            case "wasInformedBy":
                sourceNodeString = "informant";
                break;
            case "used":
                sourceNodeString = "entity";
                break;
            case "wasDerivedFrom":
                sourceNodeString = "usedEntity";
                break;
            case "wasGeneratedBy":
                sourceNodeString = "activity";
                break;
        }

        for(int i=0; i<element.getChildNodes().getLength(); i++){
            if(element.getChildNodes().item(i).getNodeName().contains(sourceNodeString)){
                result = element.getChildNodes().item(i).getAttributes().getNamedItem("prov:ref").getNodeValue();
                break;
            }
        }

        return result;
    }*/

    // https://stackoverflow.com/questions/27728550/resize-primitive-2d-array/27728645#27728645
    // It used to increase or decrease size of a matrix
    /*public Integer[][] resize(Integer[][] matrix, int w, int h) {
        Integer[][] temp = new Integer[w][h];

        for(int i=0;i<w; i++){
            for(int j=0;j<h;j++){
                temp[i][j] = 0;
            }
        }

        h = Math.min(h, matrix.length);
        w = Math.min(w, matrix[0].length);
        for (int i = 0; i < h; i++)
            System.arraycopy(matrix[i], 0, temp[i], 0, w);

        return temp;
    }*/

    /*public Integer[][] getStateCurrent() {
        return stateCurrent;
    }

    public void setStateCurrent(Integer[][] stateCurrent) {
        this.stateCurrent = stateCurrent;
    }

    public Integer[][] getStatePurge() {
        return statePurge;
    }

    public void setStatePurge(Integer[][] statePurge) {
        this.statePurge = statePurge;
    }*/

    public ArrayList<String> getRowsCurrent() {
        return rowsCurrent;
    }

    public void setRowsCurrent(ArrayList<String> rowsCurrent) {
        this.rowsCurrent = rowsCurrent;
    }

    public ArrayList<String> getColumnsCurrent() {
        return columnsCurrent;
    }

    public void setColumnsCurrent(ArrayList<String> columnsCurrent) {
        this.columnsCurrent = columnsCurrent;
    }

    public ArrayList<String> getRowsPurge() {
        return rowsPurge;
    }

    public void setRowsPurge(ArrayList<String> rowsPurge) {
        this.rowsPurge = rowsPurge;
    }

    public ArrayList<String> getColumnsPurge() {
        return columnsPurge;
    }

    public void setColumnsPurge(ArrayList<String> columnsPurge) {
        this.columnsPurge = columnsPurge;
    }

    public HashMap<String, String> getVarIdToNodeIdCurrent() {
        return varIdToNodeIdCurrent;
    }

    public void setVarIdToNodeIdCurrent(HashMap<String, String> varIdToNodeIdCurrent) {
        this.varIdToNodeIdCurrent = varIdToNodeIdCurrent;
    }

    public HashMap<String, String> getVarIdToNodeIdPurge() {
        return varIdToNodeIdPurge;
    }

    public void setVarIdToNodeIdPurge(HashMap<String, String> varIdToNodeIdPurge) {
        this.varIdToNodeIdPurge = varIdToNodeIdPurge;
    }

    public ArrayList<ArrayList<Integer>> getStateCurrent() {
        return stateCurrent;
    }

    public void setStateCurrent(ArrayList<ArrayList<Integer>> stateCurrent) {
        this.stateCurrent = stateCurrent;
    }

    public ArrayList<ArrayList<Integer>> getStatePurge() {
        return statePurge;
    }

    public void setStatePurge(ArrayList<ArrayList<Integer>> statePurge) {
        this.statePurge = statePurge;
    }
}
