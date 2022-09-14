package one.papachi.vfs4j.macfuse;

import one.papachi.macfuse4j.results.*;
import one.papachi.macfuse4j.MacFuse4j;
import one.papachi.vfs4j.VirtualFileSystem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MacFuseFileSystem extends MacFuse4j {

    private final VirtualFileSystem fileSystem;

    public MacFuseFileSystem(VirtualFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public GetattrResult getattr(String fileName) {
        if ("/".equals(fileName)) {
            return new GetattrResult(0, 040000 | 0755, 2, 1000, 1000, 0, 0, 0, 0);
        }
        try {
            VirtualFileSystem.FileInfo fileInfo = fileSystem.listFile(fileName);
            boolean isDirectory = fileInfo.isDirectory();
            int mode = isDirectory ? 040000 | 0755 : 0100000 | 0755;
            return new GetattrResult(0, mode, isDirectory ? 2 : 1, 1000, 1000, fileInfo.size(), fileInfo.atime() / 1000, fileInfo.mtime() / 1000, fileInfo.ctime() / 1000);
        } catch (Exception e) {
        }
        return new GetattrResult(-2, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public CreateResult create(String fileName, int mode) {
        int status = 0;
        try {
            fileSystem.createRegularFile(fileName);
        } catch (Exception e) {
            status = -1;
        }
        return new CreateResult(status);
    }

    @Override
    public OpenResult open(String fileName) {
        int status = 0;
        try {
            fileSystem.listFile(fileName);
        } catch (Exception e) {
            status = -2;
        }
        return new OpenResult(status);
    }

    @Override
    public ReadResult read(String fileName, ByteBuffer buffer, long offset) {
        int status = 0;
        try {
            int readTotal = 0;
            while (buffer.hasRemaining()) {
                int read = fileSystem.readFile(fileName, buffer, offset + readTotal);
                if (read == -1) {
                    break;
                }
                readTotal += read;
            }
            status = readTotal;
        } catch (Exception e) {
            status = -2;
        }
        return new ReadResult(status);
    }

    @Override
    public WriteResult write(String fileName, ByteBuffer buffer, long offset) {
        int status = 0;
        try {
            int writeTotal = 0;
            while (buffer.hasRemaining()) {
                writeTotal += fileSystem.writeFile(fileName, buffer, offset + writeTotal);
            }
            status = writeTotal;
        } catch (Exception e) {
            status = -2;
        }
        return new WriteResult(status);
    }

    @Override
    public TruncateResult truncate(String fileName, long size) {
        int status = 0;
        try {
            fileSystem.setFileSize(fileName, size);
        } catch (Exception e) {
            status = -2;
        }
        return new TruncateResult(status);
    }

    @Override
    public UnlinkResult unlink(String fileName) {
        int status = 0;
        try {
            fileSystem.deleteFile(fileName);
        } catch (Exception e) {
            status = -2;
        }
        return new UnlinkResult(status);
    }

    @Override
    public MkdirResult mkdir(String fileName) {
        int status = 0;
        try {
            fileSystem.createDirectory(fileName);
        } catch (Exception e) {
            status = -2;
        }
        return new MkdirResult(status);
    }

    @Override
    public ReaddirResult readdir(String fileName) {
        try {
            List<VirtualFileSystem.FileInfo> data = fileSystem.listDirectory(fileName);
            List<DirEntry> result = new ArrayList<>();
            for (VirtualFileSystem.FileInfo fileInfo : data) {
                boolean isDirectory = fileInfo.isDirectory();
                int mode = isDirectory ? 040000 | 0755 : 0100000 | 0755;
                result.add(new DirEntry(fileInfo.filename(), mode, isDirectory ? 2 : 1, 1000, 1000, fileInfo.size(), fileInfo.atime() / 1000, fileInfo.mtime() / 1000, fileInfo.ctime() / 1000));
            }
            return new ReaddirResult(0, result.iterator());
        } catch (Exception e) {
        }
        return new ReaddirResult(-2, Collections.emptyIterator());
    }

    @Override
    public RmdirResult rmdir(String fileName) {
        int status = 0;
        try {
            boolean isEmpty = fileSystem.isDirectoryEmpty(fileName);
            if (isEmpty) {
                fileSystem.deleteFile(fileName);
            } else {
                status = -2;
            }
        } catch (Exception e) {
            status = -2;
        }
        return new RmdirResult(status);
    }

    @Override
    public RenameResult rename(String fileName, String newFileName) {
        int status = 0;
        try {
            fileSystem.renameFile(fileName, newFileName);
        } catch (Exception e) {
            status = -2;
        }
        return new RenameResult(status);
    }

    @Override
    public ChmodResult chmod(String fileName, int mode) {
        return new ChmodResult(0);
    }

    @Override
    public ChownResult chown(String fileName, int uid, int gid) {
        return new ChownResult(0);
    }

    @Override
    public UtimentsResult utimens(String filename, long atime, long mtime) {
        return new UtimentsResult(0);
    }


}
