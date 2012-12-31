
package gphelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class SystemCommand {
    private String       command;
    private List<String> stdin  = new ArrayList<String>();  
    private List<String> stdout = new ArrayList<String>();  
    private List<String> stderr = new ArrayList<String>();  
    private boolean      ok;

    public SystemCommand() {
        this.ok = false;
    }
    
    public boolean run() {

        int i;
        
        ok = false;
        stdout.clear();
        stderr.clear();
        
        try {	
            Process p=Runtime.getRuntime().exec(command);
            if (stdin.size() > 0) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                for (i = 0; i < stdin.size(); i++) {
                    writer.write(stdin.get(i));
                    writer.newLine();
                }
                writer.close();
            }
		
            p.waitFor();

            BufferedReader reader=new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line=reader.readLine();
            while(line!=null)	{
                stderr.add(line);
                line=reader.readLine();
            }

            reader=new BufferedReader(new InputStreamReader(p.getInputStream()));
            line=reader.readLine();
            while(line!=null)	{
                stdout.add(line);
                ok = true;
                line=reader.readLine();
            }
        }
        catch(IOException e1) {
            stderr.add(e1.getMessage());
        }
        catch(InterruptedException e2) {
            stderr.add(e2.getMessage());
        }
	
        return ok;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }

    public void setStdin(List<String> stdin) {
        this.stdin = stdin;
    }

    public void setStdin(String text) {
        stdin.clear();
        String lines[] = text.split("\n");
        for (int i=0; i < lines.length; i++) {
           String line = lines[i];
           stdin.add(line);
        }
    }
    public List<String> getStdout() {
        return stdout;
    }

    public List<String> getStderr() {
        return stderr;
    }

}
