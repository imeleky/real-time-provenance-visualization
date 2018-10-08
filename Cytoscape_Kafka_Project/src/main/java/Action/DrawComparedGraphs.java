package Action;

import Base.CompareGraphsCore;
import Util.FilterUtil;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class DrawComparedGraphs {

    private CySwingAppAdapter adapter;
    private FilterUtil        filterUtil;
    private CyNetwork         network;
    private CyTable           nodeTable;
    private CompareGraphsCore compareGraphsCore;

    public DrawComparedGraphs(CompareGraphsCore compareGraphsCore){
        this.compareGraphsCore = compareGraphsCore;
        this.adapter = compareGraphsCore.getAdapter();
    }

    public void draw(ArrayList firstVector, ArrayList secondVector, JSONArray firstGraphNodes, JSONArray secondGraphNodes){
        Integer i = 0;

        visualizeNetworks();

        while(i<firstVector.size() && i<secondVector.size()){
            ArrayList nodeIDs1 = (ArrayList)(((ArrayList) firstVector.get(i)).get(0));
            ArrayList nodeIDs2 = (ArrayList)(((ArrayList) secondVector.get(i)).get(0));

            ArrayList nodeTypes1 = findNodeTypes(nodeIDs1, firstGraphNodes);
            ArrayList nodeTypes2 = findNodeTypes(nodeIDs2, secondGraphNodes);

            i++;
        }
    }

    public ArrayList findNodeTypes(ArrayList idList, JSONArray nodes){
        Integer i = 0;
        Integer j;
        ArrayList<String> nodeTypes = new ArrayList<>();

        while(i < idList.size()){
            for(j=1; j<nodes.size(); j++){

                Integer id = Integer.parseInt(((JSONObject) nodes.get(j)).get("nodeID").toString());
                if(id == (Integer)idList.get(i)){
                    String type = ((JSONObject) nodes.get(j)).get("nodeType").toString();
                    nodeTypes.add(type);
                }

            }

            i++;
        }

        return nodeTypes;
    }

    public void visualizeNetworks(){

    }

}
