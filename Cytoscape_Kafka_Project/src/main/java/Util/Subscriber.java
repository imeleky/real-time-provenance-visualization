package Util;

import App.MyControlPanel;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.*;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import redis.clients.jedis.JedisPubSub;

import javax.swing.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Subscriber extends JedisPubSub {

    private CySwingAppAdapter adapter;
    private boolean flag;
    private MyControlPanel panel;

    public Subscriber(MyControlPanel panel){
        this.panel = panel;
        this.adapter = panel.getAdapter();
        flag = false;
    }

    @Override
    public void onMessage(String channel, String message) {

        CyNetworkFactory cnf = adapter.getCyNetworkFactory();
        CyNetworkViewFactory cnvf = adapter.getCyNetworkViewFactory();
        CyNetworkViewManager networkViewManager = adapter.getCyNetworkViewManager();
        CyNetworkManager networkManager = adapter.getCyNetworkManager();
        CyNetwork myNet = null;
        CyNetworkView networkView = adapter.getCyApplicationManager().getCurrentNetworkView();

        String type = "";
        JSONObject data = new JSONObject();

        try {
            data = (JSONObject) new JSONParser().parse(message);
            type = data.get("type").toString();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(adapter.getCyApplicationManager().getCurrentNetwork() == null){
            myNet = cnf.createNetwork();
        }else {
            myNet = adapter.getCyApplicationManager().getCurrentNetwork();
        }

        networkManager.addNetwork(myNet);

        if (networkView == null) {
            // create a new view for my network
            networkView = cnvf.createNetworkView(myNet);
            networkViewManager.addNetworkView(networkView);
        }

        if(type.contains("node")){
            CyNode node = myNet.addNode();
            String nodeName = data.get("name").toString();
            String nodeID   = data.get("id").toString();
            String nodeType = data.get("nodeType").toString();

            networkView.updateView();
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("name", nodeName);
            networkView.updateView();
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("shared name", nodeName);

            if(myNet.getDefaultNodeTable().getColumn("Node ID") == null){
                myNet.getDefaultNodeTable().createColumn("Node ID", String.class, false);
            }

            if(myNet.getDefaultNodeTable().getColumn("nodeType") == null){
                myNet.getDefaultNodeTable().createColumn("nodeType", String.class, false);
            }

            if(myNet.getDefaultNodeTable().getColumn("startTime") == null){
                myNet.getDefaultNodeTable().createColumn("startTime", String.class, false);
            }

            if(myNet.getDefaultNodeTable().getColumn("endTime") == null){
                myNet.getDefaultNodeTable().createColumn("endTime", String.class, false);
            }

            if(myNet.getDefaultNodeTable().getColumn("TimeStamp") == null){
                myNet.getDefaultNodeTable().createColumn("TimeStamp", String.class, false);
            }

            if(nodeType.contains("activity")){
                if(myNet.getDefaultNodeTable().getColumn("StartDateTime") == null){
                    myNet.getDefaultNodeTable().createColumn("StartDateTime", String.class, false);
                }

                if(myNet.getDefaultNodeTable().getColumn("EndDateTime") == null){
                    myNet.getDefaultNodeTable().createColumn("EndDateTime", String.class, false);
                }

                String startDateTime    = data.get("startDateTime").toString();
                String endDateTime      = data.get("endDateTime").toString();

                myNet.getDefaultNodeTable().getRow(node.getSUID()).set("startDateTime", startDateTime);
                myNet.getDefaultNodeTable().getRow(node.getSUID()).set("endDateTime", endDateTime);

            }

            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("Node ID", nodeID);
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("nodeType", nodeType);
            myNet.getDefaultNodeTable().getRow(node.getSUID()).set("TimeStamp", Calendar.getInstance().getTime().toString());

        }else if(type.contains("edge")){

            String nodeID1          = data.get("id1").toString();
            String nodeID2          = data.get("id2").toString();
            String connectionName   = data.get("name").toString();
            String edgeType         = data.get("edgeType").toString();
            CyNode node1            = null;
            CyNode node2            = null;

            List<CyRow> nodes = adapter.getCyApplicationManager().getCurrentNetwork().getDefaultNodeTable().getAllRows();
            for(CyRow row : nodes){
                if(row.get("Node ID", String.class).equals(nodeID1)){
                    node1 = myNet.getNode(row.get(CyIdentifiable.SUID, Long.class));
                }else if(row.get("Node ID", String.class).equals(nodeID2)){
                    node2 = myNet.getNode(row.get(CyIdentifiable.SUID, Long.class));
                }
            }

            if(node1 == null || node2 == null){
                JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),
                        "There is no nodes with the given Node ID value ..!",
                        "Error!", JOptionPane.INFORMATION_MESSAGE);
                return;
            }else{
                CyEdge edge = myNet.addEdge(node1, node2, true);
                if(!isFlag()){
                    flag = true;
                }

                if(myNet.getDefaultEdgeTable().getColumn("Connection Name") == null){
                    myNet.getDefaultEdgeTable().createColumn("Connection Name", String.class, false);
                }

                if(myNet.getDefaultEdgeTable().getColumn("Interaction") == null){
                    myNet.getDefaultEdgeTable().createColumn("Interaction", String.class, false);
                }

                if(myNet.getDefaultEdgeTable().getColumn("TimeStamp") == null){
                    myNet.getDefaultEdgeTable().createColumn("TimeStamp", String.class, false);
                }

                if(myNet.getDefaultEdgeTable().getColumn("Source") == null){
                    myNet.getDefaultEdgeTable().createColumn("Source", String.class, false);
                }

                if(myNet.getDefaultEdgeTable().getColumn("Destination") == null){
                    myNet.getDefaultEdgeTable().createColumn("Destination", String.class, false);
                }

                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("Connection Name", connectionName);
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("Interaction", edgeType);
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("shared name", connectionName);
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("shared interaction", edgeType);
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("name", edgeType);
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("TimeStamp", Calendar.getInstance().getTime().toString());
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("Source", myNet.getDefaultNodeTable().getRow(node1.getSUID()).get("name", String.class));
                myNet.getDefaultEdgeTable().getRow(edge.getSUID()).set("Destination", myNet.getDefaultNodeTable().getRow(node2.getSUID()).get("name", String.class));

                networkView.updateView();
            }

        }

        NetworkViewOrganizer networkViewOrganizer = new NetworkViewOrganizer(panel);
        networkView.updateView();
        networkViewOrganizer.reOrganizeNetwork();
        panel.getInstance().getAdapter().getCyApplicationManager().getCurrentNetworkView().updateView();
    }


    @Override
    public void onPMessage(String pattern, String channel, String message) {

    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {

    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {

    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {

    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
