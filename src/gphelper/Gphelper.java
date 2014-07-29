package gphelper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;


public class Gphelper extends javax.swing.JFrame {
    
    public Gphelper() {
        initComponents();
        boolean bOk;
        String errText = "";
        SystemCommand cmd = new SystemCommand();
        gpgCommand = "gpg2";
        cmd.setCommand(gpgCommand + " --batch --version");
        bOk = cmd.run();
        if (bOk == false) {
            gpgCommand = "gpg";
            cmd.setCommand(gpgCommand + " --batch --version");
            bOk = cmd.run();
        }
        if (bOk) {
            // get public keys
            cmd.setCommand(gpgCommand + " --batch --list-keys");
            bOk = cmd.run();
            if (bOk) {
                List<String> stdout = cmd.getStdout();
                retrievePublicKeys(stdout);
            }
        }
        if (bOk) {
            // get secret keys
            cmd.setCommand(gpgCommand + " --batch --list-secret-keys");
            bOk = cmd.run();
            if (bOk) {
                List<String> stdout = cmd.getStdout();
                retrieveSecretKeys(stdout);
            }
        }
        if (bOk == false) {
            if (errText.length() == 0) {
                List<String> stderr = cmd.getStderr();
                for (String stderr1 : stderr) {
                    errText = errText + stderr1 + "\n";
                }
            }
            JOptionPane.showMessageDialog(null,errText,"GPG error",JOptionPane.ERROR_MESSAGE);
            jButtonEncrypt.setEnabled(false);
            jButtonDecrypt.setEnabled(false);
            jMenuEncryptFile.setEnabled(false);
            jMenuDecryptFile.setEnabled(false);
        }
        
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width / 2) - (size.width / 2);
        int y = (screen.height / 2) - (size.height / 2);
        setLocation(new Point(x, y));
    }
    
    private void retrievePublicKeys(List<String> list) {
        String line;
        boolean bNewKey = false;
        String  keyID   = null;
        for (String list1 : list) {
            line = list1;
            if (line.startsWith("pub")) {
                Pattern p = Pattern.compile("^.*/([^\\s]+).*");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String str = m.group(1);
                    keyID = str;
                }
                bNewKey = true;
            }
            else if (line.startsWith("uid")) {
                if (bNewKey) {
                    Pattern p = Pattern.compile("uid\\s+(.+)");
                    Matcher m = p.matcher(line);
                    if (m.matches()) {
                        String str = m.group (1);
                        publicKeysMap.put(keyID, str);
                    }
                }
                bNewKey = false;
            }
        }
    }
    
    private void retrieveSecretKeys(List<String> list) {
        String line;
        boolean bNewKey = false;
        String  keyID   = null;
        for (String list1 : list) {
            line = list1;
            if (line.startsWith("sec")) {
                Pattern p = Pattern.compile("^.*/([^\\s]+).*");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String str = m.group(1);
                    keyID = str;
                }
                bNewKey = true;
            }
            else if (line.startsWith("uid")) {
                if (bNewKey) {
                    Pattern p = Pattern.compile("uid\\s+(.+)");
                    Matcher m = p.matcher(line);
                    if (m.matches()) {
                        String str = m.group (1);
                        secretKeysMap.put(keyID, str);
                    }
                }
                bNewKey = false;
            }
        }
    }
    
    private void cut() {
        copy();
        delete();
    }
    
    private void copy() {
        String text     = jTextArea1.getText();
        int selStart    = jTextArea1.getSelectionStart();
        int selEnd      = jTextArea1.getSelectionEnd();
        
        if (selStart >= 0 && selEnd > selStart) {
            text = jTextArea1.getSelectedText();
        }
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection(text);
            clipboard.setContents(stringSelection, stringSelection);  
        } catch( HeadlessException e1) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, e1);
        } 
    }
    
    private void paste(){
        // Paste from clipboard
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            if( t!=null && t.isDataFlavorSupported(DataFlavor.stringFlavor) ) {
                String txt = (String)t.getTransferData(DataFlavor.stringFlavor);
                jTextArea1.setText(txt);
            } 
        } 
        catch(UnsupportedFlavorException e1) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, e1);
        } catch (IOException e1) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, e1);
        } 
    }

    private void delete() {
        String text     = jTextArea1.getText();
        int selStart    = jTextArea1.getSelectionStart();
        int selEnd      = jTextArea1.getSelectionEnd();
        if (selStart >= 0 && selEnd > selStart) {
            String text1 = text.substring(0, selStart);
            String text2 = text.substring(selEnd);
            text = text1 + text2; 
            jTextArea1.setText(text);
        }
        else {
            jTextArea1.setText("");
        }
    }
    
    public String getGpgText() {
        return jTextArea1.getText();
    }

    public void setGpgText(String gpgText) {
        jTextArea1.setText(gpgText);
    }

    public Map<String, String> getPublicKeysMap() {
        return publicKeysMap;
    }

    public Map<String, String> getSecretKeysMap() {
        return secretKeysMap;
    }
    
    /* 
     * Encrypt text
     */
    private void encrypt() {
        String errText      = "";
        boolean bOk         = true;
        String Text         = jTextArea1.getText();
        String clearText    = Text;
        String beforeText   = "";
        String afterText    = "";
        int selStart        = jTextArea1.getSelectionStart();
        int selEnd          = jTextArea1.getSelectionEnd();
        
        if (selStart >= 0 && selEnd > selStart) {
            clearText = jTextArea1.getSelectedText();
            beforeText = Text.substring(0, selStart);
            afterText  = Text.substring(selEnd);
        }
        
        if (clearText.length() > 0) {
            JEncryptDialog dlg = new JEncryptDialog(this, true);
            int result = dlg.showDialog();
            if (result == 1) {
                String  secretKeyId = dlg.getSelectedSecretKey();
                List<String> publicKeyIds = dlg.getSelectedPublicKeys();
                boolean bSign = dlg.isSigned();
                boolean bEncrypt = dlg.isEncrypt();
                boolean bSymmetric = dlg.isSymmetric();
                SystemCommand cmd = new SystemCommand();
                /* SYMMETRIC */
                if (bSymmetric) {
                    String passPhrase;
                    String command = gpgCommand + " --batch --quiet --armor --cipher-algo AES --symmetric";
                    passPhrase = enterPassphrase(true);
                    command = command + " --passphrase " + passPhrase;
                    cmd.setCommand(command);
                    cmd.setStdin(clearText.trim());
                    bOk = cmd.run();
                    if (bOk) {
                        String txt = "";
                        List<String> stdout = cmd.getStdout();
                        for (String stdout1 : stdout) {
                            txt = txt + stdout1 + "\n";
                        }
                        jTextArea1.setText(beforeText + txt + afterText);
                        copy();
                    }
                    else {
                        bOk = false;
                        errText = "Operation canceled.";
                    }
                }
                /* PUBLIC KEY */
                else if (bEncrypt || bSign) {
                    String command = gpgCommand + " --batch --quiet --armor --always-trust --force-mdc";
                    if (bEncrypt) {
                        command = command + " --encrypt";
                        for (String publicKeyId: publicKeyIds) {
                            command = command + " --recipient " + publicKeyId;
                        }
                    }
                    if (bSign) {
                        String password = enterPassphrase();   
                        if (password != null) {
                            command = bEncrypt ? command + " --sign" : command + " --clearsign";
                            if (secretKeyId != null) {
                                command = command + " --default-key " + secretKeyId;
                            }
                            command = command + " --passphrase " + password;
                        }
                        else {
                            bOk = false;
                            errText = "Operation canceled.";
                        }
                    }
                    if (bOk) {
                        cmd.setCommand(command);
                        cmd.setStdin(clearText.trim());
                        bOk = cmd.run();
                    }
                    if (bOk) {
                        String txt = "";
                        List<String> stdout = cmd.getStdout();
                        for (String stdout1 : stdout) {
                            txt = txt + stdout1 + "\n";
                        }
                        jTextArea1.setText(beforeText + txt + afterText);
                        copy();
                    }
                }
                else {
                    bOk = false;
                    errText = "No recipient have been selected.";
                }

                if (bOk == false) {
                    if (errText.length() == 0) {
                        List<String> stderr = cmd.getStderr();
                        for (String stderr1 : stderr) {
                            errText = errText + stderr1 + "\n";
                        }
                    }
                    JOptionPane.showMessageDialog(this,errText,"GPG error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Decrypt text
     */
    private void decrypt() {
        String startMarker  = "-----BEGIN PGP ";
        String endMarker1    = "-----END PGP MESSAGE-----";
        String endMarker2    = "-----END PGP SIGNATURE-----";
        String Text         = jTextArea1.getText();
        String cipherText   = Text;
        String beforeText   = "";
        String afterText    = "";
        int selStart        = Text.indexOf(startMarker);
        int selEnd          = Text.indexOf(endMarker1);
        int endMarkerLength = endMarker1.length();
        
        if (selEnd == -1) {
            selEnd = Text.indexOf(endMarker2);
            endMarkerLength = endMarker2.length();
        }
        
        if (selStart > -1 && selEnd > -1 && selEnd > selStart) {
            selEnd = selEnd + endMarkerLength;
            cipherText = Text.substring(selStart, selEnd);
            beforeText = Text.substring(0, selStart);
            afterText  = Text.substring(selEnd);
        }

        if (cipherText.length() > 0) {
            String password = enterPassphrase();
            if (password != null) {
                SystemCommand cmd = new SystemCommand();
                cmd.setCommand(gpgCommand + " --decrypt --quiet --batch --always-trust --passphrase " + password);
                cmd.setStdin(cipherText.trim());
                boolean bOk = cmd.run();
                if (bOk) {
                    String txt = "";
                    List<String> stdout = cmd.getStdout();
                    for (String stdout1 : stdout) {
                        txt = txt + stdout1 + "\n";
                    }
                    jTextArea1.setText(beforeText + txt + afterText);
                    List<String> stderr = cmd.getStderr();
                    for (int i = 0; i < stderr.size(); i++) {
                        if (i == 0) {
                            jTextArea1.append("\n\n");
                        }
                        jTextArea1.append(stderr.get(i) + "\n");
                    }
                }
                else {
                    String errText = "";
                    List<String> stderr = cmd.getStderr();
                    for (String stderr1 : stderr) {
                        errText = errText + stderr1 + "\n";
                    }
                    JOptionPane.showMessageDialog(this,errText,"GPG error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    private void encryptFile() {
        String errText      = "";
        boolean bOk         = true;
        String clearFilename;
        JFileChooser chooser = new JFileChooser();
        final SystemCommand cmd = new SystemCommand();
        
        //FileNameExtensionFilter filter = new FileNameExtensionFilter("All files", "*");
        //chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            clearFilename = file.getName();
            JEncryptDialog dlg = new JEncryptDialog(this, true);
            dlg.setFileEncryption(clearFilename);
            int result = dlg.showDialog();
            if (result == 1) {
                String  secretKeyId = dlg.getSelectedSecretKey();
                List<String> publicKeyIds = dlg.getSelectedPublicKeys();
                boolean bSign = dlg.isSigned();
                boolean bEncrypt = dlg.isEncrypt();
                boolean bSymmetric = dlg.isSymmetric();
                boolean bAscii = dlg.isAscii();
                String passPhrase;
                String command = gpgCommand + " --batch --quiet --yes --openpgp";
                if (bAscii) {
                    command = command + " --armor";
                }
                    command = command + " --cipher-algo AES";
                /* SYMMETRIC */
                if (bSymmetric) {
                    passPhrase = enterPassphrase(true);
                    if (passPhrase != null) {
                        command = command + " --passphrase " + passPhrase;
                        command = command + " --symmetric";
                    }
                    else {
                        bOk = false;
                        errText = "Operation canceled.";
                    }
                }
                /* PUBLIC KEY */
                else if (bEncrypt || bSign) {
                    command = command + " --always-trust --force-mdc";
                    if (bEncrypt) {
                        command = command + " --encrypt";
                        for (String publicKeyId: publicKeyIds) {
                            command = command + " --recipient " + publicKeyId;
                        }
                    }
                    if (bSign) {
                        String password = enterPassphrase();   
                        if (password != null) {
                            command = bEncrypt ? command + " --sign" : command + " --detach-sign";
                            if (secretKeyId != null) {
                                command = command + " --default-key " + secretKeyId;
                            }
                            command = command + " --passphrase " + password;
                        }
                        else {
                            bOk = false;
                            errText = "Operation canceled.";
                        }
                    }
                }
                // do
                if (bOk) {
                    command = command + " " + file.getAbsolutePath() + " ";
                    cmd.setCommand(command);
                    final JDialog loading = new JDialog(this);
                    JPanel p1 = new JPanel(new BorderLayout());
                    p1.add(new JLabel("Please wait..."), BorderLayout.CENTER);
                    loading.setUndecorated(true);
                    loading.getContentPane().add(p1);
                    loading.pack();
                    loading.setLocationRelativeTo(this);
                    loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    loading.setModal(true);
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws InterruptedException {
                            Boolean b = cmd.run();
                            return b;
                        }
                        @Override
                        protected void done() {
                            loading.dispose();
                        }
                    };
                    worker.execute();
                    loading.setVisible(true);
                    try {
                        bOk = worker.get();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Gphelper.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(Gphelper.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (bOk) {
                    /* SYMMETRIC */
                    if (bSymmetric) {
                        String txt = "";
                        List<String> stdout = cmd.getStdout();
                        for (String stdout1 : stdout) {
                            txt = txt + stdout1 + "\n";
                        }
                        txt = txt + "file " + file.getAbsolutePath() + " \nencrypted as " + file.getAbsolutePath(); 
                        txt = txt + (bAscii ? ".asc" : ".gpg");
                        txt = txt + " \nwith a symmetric key\n";
                        jTextArea1.setText(txt);
                    }
                /* PUBLIC KEY */
                    else if (bEncrypt || bSign) {
                        String txt = "";
                        List<String> stdout = cmd.getStdout();
                        for (String stdout1 : stdout) {
                            txt = txt + stdout1 + "\n";
                        }
                        txt = txt + "file " + file.getAbsolutePath() + " \n";
                        if (bEncrypt) {
                            txt = txt + "encrypted as " + file.getAbsolutePath(); 
                            txt = txt + (bAscii ? ".asc" : ".gpg") + " \n";
                            txt = txt + "with public key(s) \n";
                            for (String publicKeyId: publicKeyIds) {
                                txt = txt + publicKeyId + "\t" + publicKeysMap.get(publicKeyId) + " \n";
                            }
                        }
                        if (bSign) {
                            txt = txt + "signed ";
                            if (!bEncrypt) {
                                txt = txt + "as " + file.getAbsolutePath();
                                txt = txt + (bAscii ? ".asc" : ".sig") + " \n";
                            }
                            if (secretKeyId != null) {
                                txt = txt + " \nby secret key \n";
                                txt = txt + secretKeyId + "\t" + secretKeysMap.get(secretKeyId) + " \n";
                            }
                            else {
                                txt = txt + " \nby default secret key \n";
                            }
                        }
                        jTextArea1.setText(txt);
                    }
                }
            }
            else { // result != 1
                bOk = false;
                errText = "No recipient have been selected.";
            }

            if (bOk == false) {
                if (errText.length() == 0) {
                    List<String> stdout = cmd.getStdout();
                    for (String stdout1 : stdout) {
                        errText = errText + stdout1 + "\n";
                    }
                    List<String> stderr = cmd.getStderr();
                    for (String stderr1 : stderr) {
                        errText = errText + stderr1 + "\n";
                    }
                }
                JOptionPane.showMessageDialog(this,errText,"GPG error",JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private void decryptFile() {
        String errText          = "";
        boolean bOk             = false;
        String  inputFilename   = null;
        String  outputFilename  = null;
        File    inputFile       = null;
        File    outputFile      = null;
        JFileChooser chooser1    = new JFileChooser();
        JFileChooser chooser2    = new JFileChooser();
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter("GPG files", "gpg", "asc", "sig");
        chooser1.setFileFilter(filter);
        chooser1.setDialogTitle("Open encrypted file");
        int returnVal = chooser1.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            inputFile = chooser1.getSelectedFile();
            inputFilename = inputFile.getName();
            String password = enterPassphrase();
            if (password != null) {
                String command = gpgCommand + " --quiet --batch --yes --always-trust --passphrase " + password;
                chooser2.setDialogTitle("Save decrypted file as ...");
                returnVal = chooser2.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    outputFile = chooser2.getSelectedFile();
                    outputFilename = outputFile.getName();
                    command = command + " --output " + outputFile.getAbsolutePath();
                }
                final SystemCommand cmd = new SystemCommand();
                command = command + " --decrypt ";
                command = command + inputFile.getAbsolutePath();
                cmd.setCommand(command);
                final JDialog loading = new JDialog(this);
                JPanel p1 = new JPanel(new BorderLayout());
                p1.add(new JLabel("Please wait..."), BorderLayout.CENTER);
                loading.setUndecorated(true);
                loading.getContentPane().add(p1);
                loading.pack();
                loading.setLocationRelativeTo(this);
                loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                loading.setModal(true);
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws InterruptedException {
                        Boolean b = cmd.run();
                        return b;
                    }
                    @Override
                    protected void done() {
                        loading.dispose();
                    }
                };
                worker.execute();
                loading.setVisible(true);
                try {
                    bOk = worker.get();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Gphelper.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException ex) {
                    Logger.getLogger(Gphelper.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (bOk) {
                    String txt = "";
                    List<String> stdout = cmd.getStdout();
                    for (String stdout1 : stdout) {
                        txt = txt + stdout1 + "\n";
                    }
                    txt = txt + "\nFile " + inputFile.getAbsolutePath() + " \n";
                    txt = txt + "decrypted ";
                    txt = txt + (outputFile == null ? "OK" : "as " + outputFile.getAbsolutePath()) + ".\n";
                    List<String> stderr = cmd.getStderr();
                    for (int i = 0; i < stderr.size(); i++) {
                        if (i == 0) {
                            txt = txt + "\n\n";
                        }
                        txt = txt + stderr.get(i) + "\n";
                    }
                    jTextArea1.setText(txt);
                }
                else {
                    List<String> stderr = cmd.getStderr();
                    for (String stderr1 : stderr) {
                        errText = errText + stderr1 + "\n";
                    }
                    JOptionPane.showMessageDialog(this,errText,"GPG error",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    String enterPassphrase() {
        return enterPassphrase(false);
    }
    
    String enterPassphrase(boolean symmetric) {
        String passPhrase1;
        String passPhrase2;

        JPassPhraseDialog   dlg = new JPassPhraseDialog(this, true);
        passPhrase1 = dlg.showDialog();
        if (passPhrase1 != null && symmetric) {
            dlg.setTitle("Please repeat passphrase");
            passPhrase2 = dlg.showDialog();
            if (passPhrase2 != null) {
                if (passPhrase1.compareTo(passPhrase2) != 0) {
                    JOptionPane.showMessageDialog(this,"passphrases do not match.","GPG error",JOptionPane.ERROR_MESSAGE);
                    passPhrase1 = null;
                }
            }
        }
        return(passPhrase1);
    }
   
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupCut = new javax.swing.JMenuItem();
        jPopupCopy = new javax.swing.JMenuItem();
        jPopupPaste = new javax.swing.JMenuItem();
        jPopupDelete = new javax.swing.JMenuItem();
        jButtonEncrypt = new javax.swing.JButton();
        jButtonDecrypt = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuEncryptFile = new javax.swing.JMenuItem();
        jMenuDecryptFile = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuAbout = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuExit = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuCut = new javax.swing.JMenuItem();
        jMenuCopy = new javax.swing.JMenuItem();
        jMenuPaste = new javax.swing.JMenuItem();
        jMenuDelete = new javax.swing.JMenuItem();

        jPopupCut.setText("Cut");
        jPopupCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupCutActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupCut);

        jPopupCopy.setText("Copy");
        jPopupCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupCopyActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupCopy);

        jPopupPaste.setText("Paste");
        jPopupPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupPasteActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupPaste);

        jPopupDelete.setText("Delete");
        jPopupDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupDeleteActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupDelete);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("GnuPG Helper");

        jButtonEncrypt.setText("Encrypt/Sign");
        jButtonEncrypt.setToolTipText(" Encrypt text and copy to clipboard");
        jButtonEncrypt.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonEncrypt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEncryptActionPerformed(evt);
            }
        });

        jButtonDecrypt.setText("Decrypt/Verify");
        jButtonDecrypt.setToolTipText("Decrypt text");
        jButtonDecrypt.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonDecrypt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDecryptActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setTabSize(4);
        jTextArea1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTextArea1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTextArea1MouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(jTextArea1);

        jMenu1.setText("File");

        jMenuEncryptFile.setMnemonic('E');
        jMenuEncryptFile.setText("Encrypt File");
        jMenuEncryptFile.setToolTipText("Encrypt / Sign File");
        jMenuEncryptFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuEncryptFileActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuEncryptFile);

        jMenuDecryptFile.setMnemonic('D');
        jMenuDecryptFile.setText("Decrypt File");
        jMenuDecryptFile.setToolTipText("Decrypt / Verify File");
        jMenuDecryptFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuDecryptFileActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuDecryptFile);
        jMenu1.add(jSeparator1);

        jMenuAbout.setMnemonic('A');
        jMenuAbout.setText("About");
        jMenuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAboutActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuAbout);
        jMenu1.add(jSeparator2);

        jMenuExit.setMnemonic('x');
        jMenuExit.setText("Exit");
        jMenuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuExitActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuExit);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuCut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        jMenuCut.setText("Cut");
        jMenuCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCutActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuCut);

        jMenuCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        jMenuCopy.setText("Copy");
        jMenuCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCopyActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuCopy);

        jMenuPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        jMenuPaste.setText("Paste");
        jMenuPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuPasteActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuPaste);

        jMenuDelete.setText("Delete");
        jMenuDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuDeleteActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuDelete);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonEncrypt)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonDecrypt))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonDecrypt)
                    .addComponent(jButtonEncrypt))
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuEncryptFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuEncryptFileActionPerformed
        encryptFile();
    }//GEN-LAST:event_jMenuEncryptFileActionPerformed

    private void jMenuDecryptFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuDecryptFileActionPerformed
        decryptFile();
    }//GEN-LAST:event_jMenuDecryptFileActionPerformed

    private void jMenuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuExitActionPerformed

    private void jMenuDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuDeleteActionPerformed
        delete();
    }//GEN-LAST:event_jMenuDeleteActionPerformed

    private void jMenuCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuCutActionPerformed
        cut();
    }//GEN-LAST:event_jMenuCutActionPerformed

    private void jMenuCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuCopyActionPerformed
        copy();
    }//GEN-LAST:event_jMenuCopyActionPerformed

    private void jMenuPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuPasteActionPerformed
        paste();
    }//GEN-LAST:event_jMenuPasteActionPerformed

    private void jButtonEncryptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEncryptActionPerformed
        encrypt();
    }//GEN-LAST:event_jButtonEncryptActionPerformed

    private void jButtonDecryptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDecryptActionPerformed
        decrypt();
    }//GEN-LAST:event_jButtonDecryptActionPerformed

    private void jPopupCutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupCutActionPerformed
        cut();
    }//GEN-LAST:event_jPopupCutActionPerformed

    private void jPopupCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupCopyActionPerformed
        copy();
    }//GEN-LAST:event_jPopupCopyActionPerformed

    private void jPopupDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupDeleteActionPerformed
        delete();
    }//GEN-LAST:event_jPopupDeleteActionPerformed

    private void jPopupPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupPasteActionPerformed
        paste();
    }//GEN-LAST:event_jPopupPasteActionPerformed

    private void jTextArea1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MousePressed
        checkForTriggerEvent(evt);
    }//GEN-LAST:event_jTextArea1MousePressed

    private void jTextArea1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextArea1MouseReleased
        checkForTriggerEvent(evt);
    }//GEN-LAST:event_jTextArea1MouseReleased

    private void jMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAboutActionPerformed
        new JAboutDialog(this,true).setVisible(true);
    }//GEN-LAST:event_jMenuAboutActionPerformed
    
    private void checkForTriggerEvent(java.awt.event.MouseEvent e) {
        if ( e.isPopupTrigger() ) {
            jPopupMenu1.show( e.getComponent(), e.getX(), e.getY() );
         }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                String str = info.getName();
                if ("Nimbus".equals(info.getName())) { //Nimbus GTK+
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    //break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Gphelper.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Gphelper().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonDecrypt;
    private javax.swing.JButton jButtonEncrypt;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuCopy;
    private javax.swing.JMenuItem jMenuCut;
    private javax.swing.JMenuItem jMenuDecryptFile;
    private javax.swing.JMenuItem jMenuDelete;
    private javax.swing.JMenuItem jMenuEncryptFile;
    private javax.swing.JMenuItem jMenuExit;
    private javax.swing.JMenuItem jMenuPaste;
    private javax.swing.JMenuItem jPopupCopy;
    private javax.swing.JMenuItem jPopupCut;
    private javax.swing.JMenuItem jPopupDelete;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JMenuItem jPopupPaste;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
    private String gpgCommand;
    final private Map<String, String> publicKeysMap = new HashMap<String, String>();
    final private Map<String, String> secretKeysMap = new HashMap<String, String>();
}
