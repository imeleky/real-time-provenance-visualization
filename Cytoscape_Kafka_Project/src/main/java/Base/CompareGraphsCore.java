package Base;

import App.CytoVisProject;
import App.MyControlPanel;
import Action.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.*;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CompareGraphsCore {

    private CySwingAppAdapter adapter;
    private CytoVisProject cytoVisProject;
    private MyControlPanel myControlPanel;
    private DrawComparedGraphs drawComparedGraphs;

    public Double similarity;

    public String node1Path;
    public String node2Path;
    public String edge1Path;
    public String edge2Path;

    private String node1FileName;
    private String node2FileName;
    private String edge1FileName;
    private String edge2FileName;

    private JFileChooser fileChooser;

    JSONArray firstGrapshNodes;
    JSONArray firstGraphsEdges;
    JSONArray secondGraphsNodes;
    JSONArray secondGraphsEdges;

    ArrayList firstGraphNodeIdList;
    ArrayList secondGraphNodeIdList;

    public CompareGraphsCore(CytoVisProject cytoVisProject) {
        this.cytoVisProject = cytoVisProject;
        this.myControlPanel = cytoVisProject.getMyControlPanel();
        this.adapter = cytoVisProject.getAdapter();
        this.similarity = 1.0;

        firstGrapshNodes  = new JSONArray();
        firstGraphsEdges  = new JSONArray();
        secondGraphsNodes = new JSONArray();
        secondGraphsEdges = new JSONArray();
    }

    public boolean chooseFirstGraphsNode() {
        Boolean result = true;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose First Graph's .csv File");
        if (fileChooser.showOpenDialog(fileChooser) == 0) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.node1Path = new String();
                this.setNode1Path(file.getAbsolutePath());
                this.node2FileName = new String();
                this.setNode1FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseFirstGraphsEdge() {
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose First Graph's Edges .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.edge1Path = new String();
                this.setEdge1Path(file.getAbsolutePath());
                this.edge1FileName = new String();
                this.setEdge1FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseSecondGraphsNode() {
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose Second Graph's Nodes .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.node2Path = new String();
                this.setNode2Path(file.getAbsolutePath());
                this.node2FileName = new String();
                this.setNode2FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseSecondGraphsEdge(){
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose Second Graph's Edges .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.edge2Path = new String();
                this.setEdge2Path(file.getAbsolutePath());
                this.edge2FileName = new String();
                this.setEdge2FileName(file.getName());
            }
        }

        return result;
    }

    public Integer compareGraphs() {
        Integer result;
        ArrayList firstVector  = new ArrayList();
        ArrayList secondVector = new ArrayList();

        firstGraphsEdges  = new JSONArray();
        firstGrapshNodes  = new JSONArray();
        secondGraphsEdges = new JSONArray();
        secondGraphsNodes = new JSONArray();

        if(node1Path != null && node2Path != null && edge1Path != null && edge2Path != null){
            this.readGraphs();

            Integer firstGraphRootID  = findGraphRoot(1);
            Integer secondGraphRootID = findGraphRoot(2);
            System.out.println("Roots:" + firstGraphRootID + " " + secondGraphRootID);

            if(firstGraphRootID < 0 || secondGraphRootID < 0){
                result = -1;
            }else{
                result = 1;
                similarity = 1.0;

                firstVector  = getVector(firstGraphRootID, 1);
                secondVector = getVector(secondGraphRootID,2);

                Integer counter = 0;
                while(firstVector.size() > counter && secondVector.size() > counter){
                    ArrayList firstVectorLevel  = (ArrayList) firstVector.get(counter);
                    ArrayList secondVectorLevel = (ArrayList) secondVector.get(counter);

                    Integer firstNodeCount              = ((Integer) firstVectorLevel.get(2));
                    Integer secondNodeCount             = ((Integer) secondVectorLevel.get(2));
                    Double  firstAverageChildCount      = ((Double) firstVectorLevel.get(3));
                    Double  secondAverageChildCount     = ((Double) secondVectorLevel.get(3));

                    if(firstNodeCount > secondNodeCount){
                        similarity = similarity * (secondNodeCount.doubleValue() / firstNodeCount.doubleValue());
                    }else{
                        similarity = similarity * (firstNodeCount.doubleValue() / secondNodeCount.doubleValue());
                    }

                    if(firstAverageChildCount > 0 && secondAverageChildCount > 0){
                        if(firstAverageChildCount > secondAverageChildCount){
                            similarity = similarity * (secondAverageChildCount / firstAverageChildCount);
                        }else{
                            similarity = similarity * (firstAverageChildCount / secondAverageChildCount);
                        }
                    }

                    counter++;
                }

                if(firstVector.size() > secondVector.size()){
                    similarity = similarity * (secondVector.size() / firstVector.size());
                }else if(secondVector.size() > firstVector.size()){
                    similarity = similarity * (firstVector.size() / firstVector.size());
                }

                TaskIterator taskIterator = new TaskIterator();
                taskIterator.append(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        File file = new File(getEdge1Path());
                        LoadNetworkFileTaskFactory EdgeFile = adapter.get_LoadNetworkFileTaskFactory();
                        TaskIterator taskIterator = EdgeFile.createTaskIterator(file);
                        adapter.getTaskManager().execute(taskIterator);
                    }
                    @Override
                    public void cancel() {

                    }
                });

                taskIterator.append(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        File file = new File(getNode1Path());
                        LoadTableFileTaskFactory NodeFile = adapter.getCyServiceRegistrar().getService(LoadTableFileTaskFactory.class);
                        adapter.getTaskManager().execute(NodeFile.createTaskIterator(file));
                    }
                    @Override
                    public void cancel() {

                    }
                });

                taskIterator.append(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        File file = new File(getEdge2Path());
                        LoadNetworkFileTaskFactory EdgeFile = adapter.get_LoadNetworkFileTaskFactory();
                        TaskIterator taskIterator = EdgeFile.createTaskIterator(file);
                        adapter.getTaskManager().execute(taskIterator);
                    }
                    @Override
                    public void cancel() {

                    }
                });

                taskIterator.append(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        File file = new File(getNode2Path());
                        LoadTableFileTaskFactory NodeFile = adapter.getCyServiceRegistrar().getService(LoadTableFileTaskFactory.class);
                        adapter.getTaskManager().execute(NodeFile.createTaskIterator(file));
                    }
                    @Override
                    public void cancel() {

                    }
                });

                TaskManager taskManager = adapter.getCyServiceRegistrar().getService(TaskManager.class);
                while (taskIterator.hasNext()){
                    taskManager.execute(taskIterator);
                }

                this.drawComparedGraphs = new DrawComparedGraphs(this);
                drawComparedGraphs.draw(firstVector, secondVector, firstGrapshNodes, secondGraphsNodes);

                /*ImportEdgesAction importEdgesAction = new ImportEdgesAction(cytoVisProject, edge1Path);
                importEdgesAction.actionPerformed(new ActionEvent(new Button(), ActionEvent.ACTION_PERFORMED, null));

                ImportNodesAction importNodesAction = new ImportNodesAction(cytoVisProject, node1Path);
                importNodesAction.actionPerformed(new ActionEvent(new Button(), ActionEvent.ACTION_PERFORMED, null));
*/
                /*final JButton tempButton = new JButton();
                tempButton.setAction(new ImportEdgesAction(cytoVisProject, edge1Path));


                TaskManager taskManager = adapter.getCyServiceRegistrar().getService(TaskManager.class);
                taskManager.execute(new TaskIterator(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        tempButton.doClick();
                    }

                    @Override
                    public void cancel() {

                    }
                }));

                final JButton tempButton2 = new JButton();
                tempButton2.setAction(new ImportNodesAction(cytoVisProject, node1Path));

                taskManager.execute(new TaskIterator(new Task() {
                    @Override
                    public void run(TaskMonitor taskMonitor) throws Exception {
                        tempButton2.doClick();
                    }

                    @Override
                    public void cancel() {

                    }
                }));*/

            }

        }else{
            result = 0;
        }

        return result;
    }

    public ArrayList getVector(Integer rootID, Integer graphPointer){
        Integer levelPointer;
        Integer nodeCount;
        Integer i;
        ArrayList   vectorList = new ArrayList();
        ArrayList   queue      = new ArrayList();

        levelPointer = 1;
        nodeCount    = 1;
        queue.add(rootID);
        while (!queue.isEmpty()){
            ArrayList tmp = new ArrayList();
            tmp.add(new ArrayList());
            ArrayList<Integer> neighbours = new ArrayList<>();
            for(i=0; i<nodeCount; i++){
                neighbours.addAll(findNeighbours((Integer) (queue.get(0)), graphPointer));
                ((ArrayList)tmp.get(0)).add(queue.get(0));
                queue.remove(queue.get(0));
            }
            queue.addAll(neighbours);

            tmp.add(levelPointer);
            tmp.add(nodeCount);
            tmp.add(((Integer)neighbours.size()).doubleValue() / ((Integer)nodeCount).doubleValue());
            vectorList.add(tmp);

            levelPointer++;
            nodeCount = neighbours.size();
        }

        return vectorList;
    }

    public ArrayList<Integer> findNeighbours(Integer id, Integer graphPointer){
        ArrayList<Integer> result = new ArrayList();
        Integer i;

        if(graphPointer == 1){
            for(i=1; i<firstGraphsEdges.size(); i++){
                if(Integer.valueOf(((JSONObject)(firstGraphsEdges.get(i))).get("node1ID").toString()) == id){
                    result.add(Integer.valueOf((String)((JSONObject)(firstGraphsEdges.get(i))).get("node2ID")));
                }
            }
        }else if(graphPointer == 2){
            for(i=1; i<secondGraphsEdges.size(); i++){
                if(Integer.valueOf(((JSONObject)(secondGraphsEdges.get(i))).get("node1ID").toString()) == id){
                    result.add(Integer.valueOf((String)((JSONObject)(secondGraphsEdges.get(i))).get("node2ID")));
                }
            }
        }

        return result;
    }

    public Integer findGraphRoot(Integer graphPointer){
        Integer rootID = -1;
        Integer i;
        Integer j;
        ArrayList nodeIdList = new ArrayList<>();
        ArrayList childsList = new ArrayList<>();

        if(graphPointer == 1){
            for(i=1; i<firstGrapshNodes.size(); i++){
                nodeIdList.add(((JSONObject) (firstGrapshNodes.get(i))).get("nodeID"));
            }

            for(i=1; i<firstGraphsEdges.size(); i++){
                childsList.add(((JSONObject) (firstGraphsEdges.get(i))).get("node2ID"));
            }

            firstGraphNodeIdList = nodeIdList;
        }else if(graphPointer == 2){
            for(i=1; i<secondGraphsNodes.size(); i++){
                nodeIdList.add(((JSONObject) (secondGraphsNodes.get(i))).get("nodeID"));
            }

            for(i=1; i<secondGraphsEdges.size(); i++){
                childsList.add(((JSONObject) (secondGraphsEdges.get(i))).get("node2ID"));
            }

            secondGraphNodeIdList = nodeIdList;
        }

        ArrayList tempList = new ArrayList();
        for(i=1; i<nodeIdList.size(); i++){
            if(childsList.contains(nodeIdList.get(i))){
                for(j=1; j<nodeIdList.size(); j++){
                    if(nodeIdList.get(j) == nodeIdList.get(i)){
                        tempList.add(nodeIdList.get(j));
                    }
                }
            }
        }

        for(i=0; i<tempList.size(); i++){
            if(nodeIdList.contains(tempList.get(i))){
                nodeIdList.remove(tempList.get(i));
            }
        }

        if(nodeIdList.size() == 1){
            rootID = Integer.valueOf((String) nodeIdList.get(0));
        }

        return rootID;
    }

    public void readGraphs() {

        // Read all files
        readFile(node1Path,1);
        readFile(edge1Path,2);
        readFile(node2Path,3);
        readFile(edge2Path,4);

    }

    public void readFile(String filePath, Integer filePointer){

        String line = "";
        String csvSplitBy = ",";
        Integer i;
        JSONObject headers;
        JSONObject  row;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            headers = new JSONObject();

            if((line = br.readLine()) != null){
                String[] headersString = line.split(csvSplitBy);
                for(i=0; i< headersString.length; i++){
                    headers.put("Header" + (i+1), headersString[i]);
                }

                addToJSONArrays(headers, filePointer);
            }

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);
                row = new JSONObject();
                for(i=0; i<data.length; i++){
                    row.put(headers.get("Header" + (i+1)), data[i]);
                }

                addToJSONArrays(row, filePointer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToJSONArrays(JSONObject data, Integer filePointer){
        if(filePointer == 1){
            this.firstGrapshNodes.add(data);
        }else if(filePointer == 2){
            this.firstGraphsEdges.add(data);
        }else if(filePointer == 3){
            this.secondGraphsNodes.add(data);
        }else if(filePointer == 4){
            this.secondGraphsEdges.add(data);
        }
    }

    public void printAll(){
        JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),
                firstGraphsEdges.toString() + "\n" + firstGrapshNodes + "\n" + secondGraphsEdges + "\n" + secondGraphsNodes,
                "Error!", JOptionPane.INFORMATION_MESSAGE);
    }

    public CySwingAppAdapter getAdapter() {
        return this.adapter;
    }

    public void setAdapter(CySwingAppAdapter adapter) {
        this.adapter = adapter;
    }

    public String getNode1Path() {
        return this.node1Path;
    }

    public void setNode1Path(String node1Path) {
        this.node1Path = node1Path;
    }

    public String getNode2Path() {
        return this.node2Path;
    }

    public void setNode2Path(String node2Path) {
        this.node2Path = node2Path;
    }

    public String getEdge1Path() {
        return this.edge1Path;
    }

    public void setEdge1Path(String edge1Path) {
        this.edge1Path = edge1Path;
    }

    public String getEdge2Path() {
        return this.edge2Path;
    }

    public void setEdge2Path(String edge2Path) {
        this.edge2Path = edge2Path;
    }

    public String getNode1FileName() {
        return this.node1FileName;
    }

    public void setNode1FileName(String node1FileName) {
        this.node1FileName = node1FileName;
    }

    public String getNode2FileName() {
        return this.node2FileName;
    }

    public void setNode2FileName(String node2FileName) {
        this.node2FileName = node2FileName;
    }

    public String getEdge1FileName() {
        return this.edge1FileName;
    }

    public void setEdge1FileName(String edge1FileName) {
        this.edge1FileName = edge1FileName;
    }

    public String getEdge2FileName() {
        return this.edge2FileName;
    }

    public void setEdge2FileName(String edge2FileName) {
        this.edge2FileName = edge2FileName;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }



}
