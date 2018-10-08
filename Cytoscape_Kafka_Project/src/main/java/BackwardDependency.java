import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BackwardDependency {

    private static Integer[][] stateCurrent;
    private static Integer[][] statePurge;
    private static ArrayList<String> rowsCurrent;
    private static ArrayList<String> columnsCurrent;
    private static ArrayList<String> rowsPurge;
    private static ArrayList<String> columnsPurge;
    private static HashMap<String, String> varIdToNodeIdCurrent;
    private static HashMap<String, String> varIdToNodeIdPurge;
    private static ArrayList<String> relations = new ArrayList<String>(Arrays.asList(
            "prov:wasAssociatedWith", "prov:wasInformedBy", "prov:used", "prov:wasDerivedFrom", "prov:wasGeneratedBy"));
    private static Integer componentCount;


    public static void main(String[] args){
        componentCount          = 0;
        stateCurrent            = new Integer[componentCount][componentCount];
        statePurge              = new Integer[componentCount][componentCount];
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

    }

    public static void printMatrix(Integer[][] matrix){
        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[i].length; j++){
                System.out.println(matrix[i][j] + " ");
            }
            System.out.print("\n");
        }
    }

    public static void updateState(Node element, String node1Id, String node2Id){
        String sourceNode;
        String destNode;
        ArrayList<String> inputVars;

        // Check if this is an edge
        if(relations.contains(element.getNodeName())){
            // get source and destination nodes
            sourceNode   = getSourceNodeId(element, "wasAssociatedWith");
            destNode     = getDestNodeId(element, "wasAssociatedWith");

            if(varIdToNodeIdCurrent.containsKey(sourceNode) && varIdToNodeIdCurrent.get(sourceNode) != node1Id){
                cacheDependencies();

                for(int i=0; i<componentCount; i++){
                    if(rowsCurrent.get(i).equals(sourceNode)){
                        rowsCurrent.remove(sourceNode);
                        stateCurrent = removeRow(i, stateCurrent);
                    }
                }
            }

            // Üstteki if'in içine?
            varIdToNodeIdCurrent.put(sourceNode, node1Id);

            if(columnsCurrent.contains(destNode)){
                // Üstteki if'in else kısmına?
                rowsCurrent.add(sourceNode);
                columnsCurrent.add(destNode);
                componentCount++;
                stateCurrent = resize(stateCurrent, componentCount, componentCount);
                stateCurrent[rowsCurrent.size()-1][columnsCurrent.size()-1] = 1;
            }

            if(sourceNode.equals(destNode)){
                inputVars = getBackwardProvenance(destNode, statePurge, rowsPurge, columnsPurge);
            }else {
                inputVars = getBackwardProvenance(destNode,stateCurrent, rowsCurrent, columnsCurrent);
            }

            for(String var : inputVars){
                stateCurrent[rowsCurrent.indexOf(sourceNode)][columnsCurrent.indexOf(var)] = 1;
            }

        }

    }

    // Bütün bir satır çekilemez mi??
    public static ArrayList<String> getBackwardProvenance(String varId, Integer[][] matrix, ArrayList<String> sourceList, ArrayList<String> columns){
        ArrayList<String> result    = new ArrayList<>();
        Integer varIndex            = sourceList.indexOf(varId);

        for(int i=0; i<varIndex; i++){
            if(matrix[varIndex][i] == 1){
                result.add(columns.get(i));
            }
        }

        return result;
    }

    public static void cacheDependencies(){
        rowsPurge       = rowsCurrent;
        columnsPurge    = columnsCurrent;
        statePurge      = stateCurrent;
    }

    // Remove a row from a matrix
    public static Integer[][] removeRow(Integer i, Integer[][] matrix){
        for(int j=i; j<matrix.length-1; j++){
            matrix[j] = matrix[j+1];
        }

        return matrix;
    }

    // Find source node Id
    public static String getSourceNodeId(Node element, String relationType){
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
    }

    // Find destination node Id
    public static String getDestNodeId(Node element, String relationType){
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
    }

    // https://stackoverflow.com/questions/27728550/resize-primitive-2d-array/27728645#27728645
    // It used to increase or decrease size of a matrix
    public static Integer[][] resize(Integer[][] matrix, int w, int h) {
        Integer[][] temp = new Integer[h][w];
        h = Math.min(h, matrix.length);
        w = Math.min(w, matrix[0].length);
        for (int i = 0; i < h; i++)
            System.arraycopy(matrix[i], 0, temp[i], 0, w);

        return temp;
    }

    public static Integer[][] getStateCurrent() {
        return stateCurrent;
    }

    public static void setStateCurrent(Integer[][] stateCurrent) {
        BackwardDependency.stateCurrent = stateCurrent;
    }

    public static Integer[][] getStatePurge() {
        return statePurge;
    }

    public static void setStatePurge(Integer[][] statePurge) {
        BackwardDependency.statePurge = statePurge;
    }

    public static ArrayList<String> getRowsCurrent() {
        return rowsCurrent;
    }

    public static void setRowsCurrent(ArrayList<String> rowsCurrent) {
        BackwardDependency.rowsCurrent = rowsCurrent;
    }

    public static ArrayList<String> getColumnsCurrent() {
        return columnsCurrent;
    }

    public static void setColumnsCurrent(ArrayList<String> columnsCurrent) {
        BackwardDependency.columnsCurrent = columnsCurrent;
    }

    public static ArrayList<String> getRowsPurge() {
        return rowsPurge;
    }

    public static void setRowsPurge(ArrayList<String> rowsPurge) {
        BackwardDependency.rowsPurge = rowsPurge;
    }

    public static ArrayList<String> getColumnsPurge() {
        return columnsPurge;
    }

    public static void setColumnsPurge(ArrayList<String> columnsPurge) {
        BackwardDependency.columnsPurge = columnsPurge;
    }

    public static HashMap<String, String> getVarIdToNodeIdCurrent() {
        return varIdToNodeIdCurrent;
    }

    public static void setVarIdToNodeIdCurrent(HashMap<String, String> varIdToNodeIdCurrent) {
        BackwardDependency.varIdToNodeIdCurrent = varIdToNodeIdCurrent;
    }

    public static HashMap<String, String> getVarIdToNodeIdPurge() {
        return varIdToNodeIdPurge;
    }

    public static void setVarIdToNodeIdPurge(HashMap<String, String> varIdToNodeIdPurge) {
        BackwardDependency.varIdToNodeIdPurge = varIdToNodeIdPurge;
    }

    public static ArrayList<String> getRelations() {
        return relations;
    }

    public static void setRelations(ArrayList<String> relations) {
        BackwardDependency.relations = relations;
    }

    public static Integer getComponentCount() {
        return componentCount;
    }

    public static void setComponentCount(Integer componentCount) {
        BackwardDependency.componentCount = componentCount;
    }
}