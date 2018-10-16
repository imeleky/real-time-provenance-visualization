package Action;

import App.CytoVisProject;
import Util.BackwardDependency;
import com.opencsv.CSVReader;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ImportEdgesRightClickAction implements MouseListener {
    // Variables
    private CySwingAppAdapter adapter;
    private CytoVisProject cytoVisProject;
    private BackwardDependency backwardDependency;
    private File file;

    public ImportEdgesRightClickAction(CytoVisProject cytoVisProject, BackwardDependency backwardDependency){
        // Initializations of Variables
        this.cytoVisProject     = cytoVisProject;
        this.adapter            = cytoVisProject.getAdapter();
        this.backwardDependency = backwardDependency;
    }

    public void mouseClicked(MouseEvent e) {
        if(SwingUtilities.isRightMouseButton(e)){
            // Making a choice to the user for file selection
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose Network File");
            if(fileChooser.showOpenDialog(fileChooser) == JFileChooser.APPROVE_OPTION){
                file = fileChooser.getSelectedFile();
            }

            LoadNetworkFileTaskFactory EdgeFile = adapter.get_LoadNetworkFileTaskFactory();
            TaskIterator taskIterator = EdgeFile.createTaskIterator(file);
            adapter.getTaskManager().execute(taskIterator);
            cytoVisProject.getMyControlPanel().setStatus("Network is loaded.");
            cytoVisProject.getMyControlPanel().importTableButton.setEnabled(true);

            try {
                if(fileChooser.getSelectedFile().getPath().contains(".csv")){
                    FileReader filereader       = new FileReader(file);
                    CSVReader csvReader         = new CSVReader(filereader);
                    JSONArray data              = new JSONArray();
                    ArrayList<String> headers   = new ArrayList<>();
                    JSONParser parser           = new JSONParser();
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

                    long startTime = new Date().getTime();

                    for(Object object : data){
                        temp = (JSONObject) parser.parse(object.toString());
                        backwardDependency.updateState(temp.get("source").toString(), temp.get("dest").toString());
                    }

                    System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Total time to run BDM: "
                            + (new Date().getTime() - startTime) + " ms.");
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }
}