package Action;

import App.CytoVisProject;
import Util.BackwardDependency;
import com.opencsv.CSVReader;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyRow;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ImportEdgesAction extends AbstractCyAction {
    // Variables
    private CySwingAppAdapter adapter;
    private CytoVisProject cytoVisProject;
    private BackwardDependency backwardDependency;
    private File file;
    private String path;

    public ImportEdgesAction(CytoVisProject cytoVisProject, String path){
        // Initializations of Variables
        super("<html>Import<br/>Network</html>");
        this.cytoVisProject         = cytoVisProject;
        this.adapter                = cytoVisProject.getAdapter();
        this.path                   = path;
        this.file                   = new File(path);
        this.backwardDependency     = new BackwardDependency();
    }

    public void actionPerformed(ActionEvent e){
        System.out.println("Path: " + path);

        // Loading edges which has been extracted
        if (file == null){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),"File does not" +
                    "exist","Error",JOptionPane.INFORMATION_MESSAGE);
        }else{
            try {
                LoadNetworkFileTaskFactory EdgeFile = adapter.get_LoadNetworkFileTaskFactory();
                TaskIterator taskIterator = EdgeFile.createTaskIterator(file);
                adapter.getTaskManager().execute(taskIterator);
                cytoVisProject.getMyControlPanel().setStatus("Network is loaded.");
                cytoVisProject.getMyControlPanel().importTableButton.setEnabled(true);

                if(path.contains(".csv")){
                    FileReader filereader       = new FileReader(file);
                    CSVReader csvReader         = new CSVReader(filereader);
                    JSONArray data              = new JSONArray();
                    ArrayList<String> headers   = new ArrayList<>();
                    JSONObject temp;
                    String[] nextRecord;


                    nextRecord = csvReader.readNext();
                    if(nextRecord != null){
                        for (String header : nextRecord) {
                            headers.add(header);
                        }

                        Integer counter;
                        while ((nextRecord = csvReader.readNext()) != null) {
                            temp    = new JSONObject();
                            counter = 0;

                            for (String cell : nextRecord) {
                                temp.put(headers.get(counter++), cell);
                            }

                            data.add(temp);
                        }
                    }

                    for(Object object : data){
                        JSONObject line = (JSONObject) object;
                        backwardDependency.updateState(line.get("source").toString(), line.get("dest").toString());
                    }

                    long startTime = new Date().getTime();

                    for(CyRow row : adapter.getCyApplicationManager().getCurrentNetwork().getDefaultEdgeTable().getAllRows()){
                        backwardDependency.updateState(row.get("Source", String.class), row.get("Destination", String.class));
                    }

                    System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Total time to run BDM: "
                            + (new Date().getTime() - startTime));
                }
            }catch (Exception es){
                JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),"The files that" +
                        " you choosed are not valid!","error", JOptionPane.INFORMATION_MESSAGE);
                es.printStackTrace();
            }
        }
    }
}