package Base;

import App.CytoVisProject;
import Util.BackwardDependency;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EdgesAddedListener implements AddedEdgesListener {

    private CytoVisProject cytoVisProject;
    private BackwardDependency backwardDependency;
    private CySwingAppAdapter adapter;

    public EdgesAddedListener(CytoVisProject cytoVisProject){
        this.cytoVisProject     = cytoVisProject;
        this.backwardDependency = cytoVisProject.getMyControlPanel().getBackwardDependency();
        this.adapter            = cytoVisProject.getMyControlPanel().getAdapter();
    }

    @Override
    public void handleEvent(AddedEdgesEvent addedEdgesEvent) {
        String sourceNodeId         = new String();
        String destNodeId           = new String();
        CyTable currentEdgeTable    = adapter.getCyApplicationManager().getCurrentNetwork().getDefaultEdgeTable();

        long startTime = new Date().getTime();

        for (CyEdge edge : addedEdgesEvent.getPayloadCollection()){
            backwardDependency.updateState(currentEdgeTable.getRow(edge.getSUID()).get("Source", String.class), currentEdgeTable.getRow(edge.getSUID()).get("Destination", String.class));
        }

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Total time to run BDM: "
                + (new Date().getTime() - startTime));
    }
}
