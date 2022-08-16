/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetAction;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.administration.configeditor.BackupRecord;
import io.goobi.viewer.model.administration.configeditor.FileRecord;
import io.goobi.viewer.model.administration.configeditor.FilesListing;

@ManagedBean
@SessionScoped
public class ConfigEditorBean implements Serializable {

    private static final long serialVersionUID = -4120457702630667052L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigEditorBean.class);

    /** Manual edit locks for files. */
    private static final Map<Path, String> fileLocks = new HashMap<>();

    /** Object that handles the reading of listed failes. */
    private final FilesListing filesListing = new FilesListing();

    // Fields for FileEdition
    private int fileInEditionNumber;
    private FileRecord currentFileRecord;
    private String fileContent;
    private String temp = ""; // Used to check if the content of the textarea is modified

    // ReadOnly or editable
    private boolean editable = false;

    // Fields for Backups
    private List<BackupRecord> backupRecords;
    private DataModel<BackupRecord> backupRecordsModel;
    private File[] backupFiles;
    private String[] backupNames;
    private String backupsPath; // to maintain the backup files

    // Whether there is anything to download
    private boolean downloadable = false;

    // Fields for File-IO
    //    private transient FileOutputStream fileOutputStream = null;
    private transient FileLock inputLock = null;
    //    private transient FileLock outputLock = null;

    // Whether the opened config file is "config_viewer.xml"
    private boolean isConfigViewer;

    // Used to render the CodeMirror editor properly, values can be "properties" or "xml"
    private String currentConfigFileType;
    private String fullCurrentConfigFileType; // "." + currentConfigFileType

    private boolean nightMode = false;

    public ConfigEditorBean() {
        //
    }

    @PostConstruct
    public void init() {
        if (!DataManager.getInstance().getConfiguration().isConfigEditorEnabled()) {
            // Give a message that the config-editor is not activated.
            logger.warn("The ConfigEditor is not activated!");
            return;
        }

        // Create the folder "backups" if necessary.
        backupsPath = FileTools.adaptPathForWindows(DataManager.getInstance().getConfiguration().getConfigLocalPath() + "backups/");
        File backups = new File(backupsPath);
        if (!backups.exists()) {
            backups.mkdir();
        }

        // Initialize Backup List 
        backupFiles = null;
        backupNames = null;
        backupRecords = new ArrayList<>(Arrays.asList(
                new BackupRecord("No Backup File Found", -1)));
        backupRecordsModel = new ListDataModel<>(backupRecords);
    }

    public boolean isRenderBackend() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    public void refresh() {
        filesListing.refresh();
    }

    public DataModel<FileRecord> getFileRecordsModel() {
        return filesListing.getFileRecordsModel();
    }

    public String[] getFileNames() {
        return filesListing.getFileNames();
    }

    /**
     * 
     * @return Name of the file that corresponds to fileInEditionNumber
     */
    public String getFileInEditionName() {
        if (fileInEditionNumber != -1 && filesListing.getFileNames().length > fileInEditionNumber) {
            return filesListing.getFileNames()[fileInEditionNumber];
        }

        return "";
    }

    public int getFileInEditionNumber() {
        return fileInEditionNumber;
    }

    public void setFileInEditionNumber(int fileInEditionNumber) {
        this.fileInEditionNumber = fileInEditionNumber;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public List<BackupRecord> getBackupRecords() {
        return backupRecords;
    }

    public DataModel<BackupRecord> getBackupRecordsModel() {
        return backupRecordsModel;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isDownloadable() {
        return downloadable;
    }

    public void setDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
    }

    public String getCurrentConfigFileType() {
        return currentConfigFileType;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void changeNightMode() {
        nightMode = !nightMode;
    }

    ///////////////////// Hidden Text /////////////////////////
    private String hiddenText = "Hi, how are you doing?";

    public String getHiddenText() {
        return hiddenText;
    }
    ///////////////////////////////////////////////////////////

    public synchronized void openFile() throws IOException {
        if (fileInEditionNumber < 0) {
            return;
        }

        currentFileRecord = filesListing.getFileRecords().get(fileInEditionNumber);

        Path filePath = filesListing.getFiles()[fileInEditionNumber].toPath();
        String sessionId = BeanUtils.getSession().getId();
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            FileChannel inputChannel = fis.getChannel();

            // Release write lock
            if (fileLocks.containsKey(filePath) && fileLocks.get(filePath).equals(sessionId)) {
                fileLocks.remove(filePath);
            }

            //            if (fileOutputStream != null) {
            //                if (outputLock.isValid()) {
            //                    outputLock.release();
            //                }
            //                fileOutputStream.close();
            //            }

            // get an exclusive lock if the file is editable, otherwise a shared lock
            if (editable) {

                // File already locked by someone else
                if (fileLocks.containsKey(filePath) && !fileLocks.get(filePath).equals(sessionId)) {
                    Messages.error("admin__config_editor__file_locked_msg");
                    return;
                }
                fileLocks.put(filePath, sessionId);
                logger.trace("{} locked for session ID {}", filePath.toAbsolutePath(), sessionId);
                // outputLock also locks reading this file in Windows, so read it prior to creating the lock
                fileContent = Files.readString(filePath);
                //                fileOutputStream = new FileOutputStream(pathString, false); // appending instead of covering 
                //                outputLock = fileOutputStream.getChannel().tryLock();
                //                if (outputLock == null) {
                //                    throw new OverlappingFileLockException();
                //                }
            } else { // READ_ONLY
                inputLock = inputChannel.tryLock(0, Long.MAX_VALUE, true);
                if (inputLock == null) {
                    throw new OverlappingFileLockException();
                }
                fileContent = Files.readString(filePath);
            }
            temp = fileContent;
        } catch (OverlappingFileLockException oe) {
            logger.trace("The region specified is already locked by another process.", oe);

        } catch (IOException e) {
            logger.trace("IOException caught in the method openFile()", e);
        } finally {
            if (inputLock != null && inputLock.isValid()) {
                inputLock.release();
            }
        }

        hiddenText = "New File Chosen!";
    }

    public void editFile(boolean writable) {
        showBackups(writable);
    }

    /**
     * Saves the currently open file.
     * 
     * @return Navigation outcome
     */
    public synchronized String saveCurrentFileAction() {
        logger.trace("saveCurrentFileAction");
        // No need to duplicate if no modification is done.
        //        if (temp.equals(fileContent)) {
        //            hiddenText = "No Modification Detected!";
        //            return "";
        //        }
        //        if (fileOutputStream == null) {
        //            logger.error("No FileOutputStream");
        //            return "";
        //        }

        logger.trace("fileContent:\n{}", fileContent);
        // Save the latest modification to the original path.
        Path originalPath = Path.of(filesListing.getFiles()[fileInEditionNumber].getAbsolutePath());

        // Abort if file locked by someone else
        if (fileLocks.containsKey(originalPath) && !fileLocks.get(originalPath).equals(BeanUtils.getSession().getId())) {
            Messages.error("admin__config_editor__file_locked_msg");
            return "";
        }

        // Use the filename without extension to create a folder for its backup_copies.
        String newBackupFolderPath = backupsPath + filesListing.getFiles()[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", "");
        File newBackupFolder = new File(newBackupFolderPath);
        if (!newBackupFolder.exists()) {
            newBackupFolder.mkdir();
        }

        try {
            Files.writeString(originalPath, fileContent, StandardCharsets.UTF_8);
            // In Windows, the exact same stream/channel that holds the outputLock must be used to write to avoid IOException
            //            IOUtils.write(fileContent, fileOutputStream, StandardCharsets.UTF_8);
            // if the "config_viewer.xml" is being edited, then the original content of the block <configEditor> should be written back
            if (isConfigViewer) {
                logger.debug("Saving {}, changes to config editor settings will be reverted...", Configuration.CONFIG_FILE_NAME);
                boolean origConfigEditorEnabled = DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
                int origConfigEditorMax = DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups();
                List<String> origConfigEditorDirectories = DataManager.getInstance().getConfiguration().getConfigEditorDirectories();

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
                Document document = documentBuilder.parse(originalPath.toFile());
                document.getDocumentElement().normalize();

                // get the parent node <configEditor>
                Node configEditor = document.getElementsByTagName("configEditor").item(0);

                // set the values of the attributes "enabled" and "maximum" back
                configEditor.getAttributes().getNamedItem("enabled").setNodeValue(String.valueOf(origConfigEditorEnabled));
                configEditor.getAttributes().getNamedItem("maximum").setNodeValue(String.valueOf(origConfigEditorMax));

                // get the list of all <directory> elements
                NodeList directoryList = configEditor.getChildNodes();

                // remove these modified elements
                while (directoryList.getLength() > 0) {
                    Node node = directoryList.item(0);
                    configEditor.removeChild(node);
                }
                // rewrite the backed-up values "configPaths" into this block
                for (String configPath : origConfigEditorDirectories) {
                    Node newNode = document.createElement("directory");
                    newNode.setTextContent(configPath);
                    configEditor.appendChild(document.createTextNode("\n\t"));
                    configEditor.appendChild(newNode);
                }
                configEditor.appendChild(document.createTextNode("\n    "));

                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                DOMSource src = new DOMSource(document);
                StreamResult result = new StreamResult(originalPath.toFile());
                transformer.transform(src, result);

                // save the modified content again
                fileContent = Files.readString(originalPath);

                if (temp.equals(fileContent)) {
                    hiddenText = "No Valid Modification Detected!";
                    return "";
                }
            }

            // Use a time stamp to distinguish the backups.
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS").format(new java.util.Date()).replace(":", "").replaceFirst("[.]", "");
            Path newBackupPath = Path.of(newBackupFolderPath + "/" + filesListing.getFiles()[fileInEditionNumber].getName() + "." + timeStamp);
            // save the original content to backup files
            Files.writeString(newBackupPath, temp, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.trace("IOException caught in the method saveFile()", e);
        } catch (SAXException e) {
            logger.trace("SAXException caught in the method saveFile()", e);
        } catch (ParserConfigurationException e) {
            logger.trace("ParserConfigurationException caught in the method saveFile()", e);
        } catch (TransformerConfigurationException e) {
            logger.trace("TransformerConfigurationException caught in the method saveFile()", e);
        } catch (TransformerException e) {
            logger.trace("TransformerException caught in the method saveFile()", e);
        } finally {
            fileLocks.remove(originalPath);
            logger.trace("{} lock removed", originalPath.toAbsolutePath());
            //            if (outputLock != null && outputLock.isValid()) {
            //                outputLock.release();
            //            }
            //            fileOutputStream.close();
            refresh();
        }

        temp = fileContent;

        // refresh the backup metadata
        backupFiles = newBackupFolder.listFiles();
        Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // last modified comes on top

        int length = backupFiles.length;
        if (DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups() > 0
                && length > DataManager.getInstance().getConfiguration().getConfigEditorMaximumBackups()) {
            // remove the oldest backup
            try {
                Files.delete(backupFiles[length - 1].toPath());
                length -= 1;
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        backupNames = new String[length];
        backupRecords = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
            backupRecords.add(new BackupRecord(backupNames[i], i));
        }
        backupRecordsModel = new ListDataModel<>(backupRecords);
        downloadable = true;

        hiddenText = "File saved!";
        Messages.info("updatedSuccessfully");

        return "";
    }

    public void showBackups() {
        showBackups(false);
    }

    /**
     * 
     * @param writable
     */
    public void showBackups(boolean writable) {
        FileRecord rec = filesListing.getFileRecordsModel().getRowData();
        isConfigViewer = rec.getFileName().equals(Configuration.CONFIG_FILE_NAME); // Modifications of "config_viewer.xml" should be limited
        currentConfigFileType = rec.getFileType();
        fullCurrentConfigFileType = ".".concat(currentConfigFileType);

        fileInEditionNumber = rec.getNumber();
        editable = writable;

        logger.info("fileInEditionNumber: {}; fileName: {}", fileInEditionNumber, rec.getFileName());

        File backups = new File(backupsPath + filesListing.getFiles()[fileInEditionNumber].getName().replaceFirst("[.][^.]+$", ""));
        if (backups.exists()) {
            backupFiles = backups.listFiles();
            Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // last modified comes on top
            backupNames = new String[backupFiles.length];
            backupRecords = new ArrayList<>();
            for (int i = 0; i < backupFiles.length; ++i) {
                backupNames[i] = backupFiles[i].getName().replaceFirst(".+?(?=([0-9]+))", "").replaceFirst(fullCurrentConfigFileType, "");
                backupRecords.add(new BackupRecord(backupNames[i], i));
            }
            downloadable = true;

        } else {
            backupFiles = null;
            backupNames = null;
            backupRecords = new ArrayList<>(Arrays.asList(
                    new BackupRecord("No Backup File Found", -1)));
            downloadable = false;
        }
        backupRecordsModel = new ListDataModel<>(backupRecords);

        try {
            openFile();
        } catch (IOException e) {
            logger.trace("IOException caught in the method showBackups(boolean)", e);
        }
    }

    /**
     * @param rec {@link BackupRecord} for which to download the file
     * @return Navigation outcome
     * @throws IOException
     */
    public String downloadFile(BackupRecord rec) throws IOException {
        logger.trace("downloadFile: {}", rec != null ? rec.getName() : "null");
        if (rec == null) {
            throw new IllegalArgumentException("rec may not be null");
        }

        File backupFile = new File(backupFiles[rec.getNumber()].getAbsolutePath());
        String fileName = filesListing.getFileNames()[fileInEditionNumber].concat(".").concat(rec.getName());

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext ec = facesContext.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType("text/".concat(currentConfigFileType));
        ec.setResponseHeader("Content-Length", String.valueOf(Files.size(backupFile.toPath())));
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        try (OutputStream outputStream = ec.getResponseOutputStream(); FileInputStream fileInputStream = new FileInputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (GetAction.isClientAbort(e)) {
                logger.trace("Download of '{}' aborted: {}", fileName, e.getMessage());
                return "";
            }
            throw e;
        }
        facesContext.responseComplete();
        hiddenText = "File downloaded!";
        return "";
    }

    /**
     * TODO
     */
    public String cancelAction() {
        logger.trace("cancel");
        try {
            
            fileContent = null;
            // Release read lock
            if (inputLock != null && inputLock.isValid()) {
                inputLock.release();
            }
            // Release write lock
            if (fileInEditionNumber >= 0) {
                Path filePath = filesListing.getFiles()[fileInEditionNumber].toPath();
                if (fileLocks.containsKey(filePath) && fileLocks.get(filePath).equals(BeanUtils.getSession().getId())) {
                    fileLocks.remove(filePath);
                }
            }

            fileInEditionNumber = -1;

            refresh();
            openFile();
        } catch (Exception e) {
            logger.trace("Exception caught in cancelEdition()", e);
        }

        return "";
    }

    /**
     * Removes file locks for the given session id.
     * 
     * @param sessionId
     */
    public static void clearLocksForSessionId(String sessionId) {
        Set<Path> toClear = new HashSet<>();
        for (Entry<Path, String> entry : fileLocks.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                toClear.add(entry.getKey());
            }
        }
        if (!toClear.isEmpty()) {
            for (Path path : toClear) {
                fileLocks.remove(path);
                logger.debug("Released edit lock for {}", path.toAbsolutePath());
            }
        }
    }
}
