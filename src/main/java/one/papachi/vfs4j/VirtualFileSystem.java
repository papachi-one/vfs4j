package one.papachi.vfs4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystemException;
import java.util.List;

public interface VirtualFileSystem {

    List<FileInfo> listDirectory(String filename) throws FileSystemException, IOException;

    FileInfo listFile(String filename) throws FileSystemException, IOException;

    boolean isDirectoryEmpty(String filename) throws FileSystemException, IOException;

    void createDirectory(String filename) throws FileSystemException, IOException;

    void createRegularFile(String filename) throws FileSystemException, IOException;

    void renameFile(String filename, String newFilename) throws FileSystemException, IOException;

    void deleteFile(String filename) throws FileSystemException, IOException;

    void openFile(String filename, boolean write) throws FileSystemException, IOException;

    void closeFile(String filename) throws FileSystemException, IOException;

    int readFile(String filename, ByteBuffer buffer, long position) throws FileSystemException, IOException;

    int writeFile(String filename, ByteBuffer buffer, long position) throws FileSystemException, IOException;

    void setFileSize(String filename, long size) throws FileSystemException, IOException;

    record FileInfo(String filename, boolean isDirectory, long size, int uid, int gid, long atime, long mtime, long ctime) {
    }

}
