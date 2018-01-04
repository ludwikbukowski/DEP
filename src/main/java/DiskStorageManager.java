import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ludwikbukowski on 27/12/17.
 */
public class DiskStorageManager {
    private  ObjectOutputStream oos = null;
    private FileOutputStream fout = null;
    private  ObjectInputStream objectinputstream = null;
    private  FileInputStream streamIn =  null;
    private  ArrayList<Msg> archive = new ArrayList<Msg>();
    private String node;
    private String createFileName(Integer Id){
        return "./node" + Id + ".backup";
    }


    public void setNode(int node){
        this.node = createFileName(node);
    }
    public void write(Msg msg) throws IOException {
        fout = new FileOutputStream(node, false);
        oos = new ObjectOutputStream(fout);
        archive.add(msg);
        oos.writeObject(archive);
        oos.flush();
        oos.close();
        fout.close();
    }

    public void clear() throws IOException {
        fout = new FileOutputStream(node, false);
        oos = new ObjectOutputStream(fout);
        oos.close();
        fout.close();
    }

    public boolean isEmpty(Integer Id) throws IOException {
        node = createFileName(Id);
        streamIn = new FileInputStream(createFileName(Id));
        objectinputstream = new ObjectInputStream(streamIn);
        return !(objectinputstream.available() > 0);
    }

    public List<Msg> read(Integer Id)  {
        node = createFileName(Id);
        try {
        streamIn = new FileInputStream(createFileName(Id));
        objectinputstream = new ObjectInputStream(streamIn);
            archive.clear();
            archive = (ArrayList<Msg>) objectinputstream.readObject();
            objectinputstream .close();
            streamIn.close();
        } catch (FileNotFoundException e) {
            //
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(objectinputstream != null){

            }
        }

        return archive;
    }
}
