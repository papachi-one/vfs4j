package one.papachi.vfs4j.dokany;

import one.papachi.dokany4j.Dokany4j;
import one.papachi.dokany4j.constants.CreationDisposition;
import one.papachi.dokany4j.constants.DesiredAccess;
import one.papachi.dokany4j.constants.FileAttributes;
import one.papachi.dokany4j.constants.NtStatus;
import one.papachi.dokany4j.constants.ShareAccess;
import one.papachi.dokany4j.results.CreateFileResult;
import one.papachi.dokany4j.results.DeleteDirectoryResult;
import one.papachi.dokany4j.results.DeleteFileResult;
import one.papachi.dokany4j.results.DokanFileInfo;
import one.papachi.dokany4j.results.FindData;
import one.papachi.dokany4j.results.FindFilesResult;
import one.papachi.dokany4j.results.FlushFileBuffers;
import one.papachi.dokany4j.results.GetDiskFreeSpaceResult;
import one.papachi.dokany4j.results.GetFileInformationResult;
import one.papachi.dokany4j.results.GetFileSecurityResult;
import one.papachi.dokany4j.results.GetVolumeInformationResult;
import one.papachi.dokany4j.results.MountedResult;
import one.papachi.dokany4j.results.MoveFileResult;
import one.papachi.dokany4j.results.ReadFileResult;
import one.papachi.dokany4j.results.SetAllocationSizeResult;
import one.papachi.dokany4j.results.SetEndOfFileResult;
import one.papachi.dokany4j.results.SetFileAttributesResult;
import one.papachi.dokany4j.results.SetFileTimeResult;
import one.papachi.dokany4j.results.WriteFileResult;
import one.papachi.vfs4j.VirtualFileSystem;
import one.papachi.vfs4j.VirtualFileSystem.FileInfo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class  DokanyFileSystem extends Dokany4j {

    private static final String ROOT = "/";

    private final VirtualFileSystem fileSystem;

    public DokanyFileSystem(VirtualFileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public CreateFileResult createFile(String fileName, int genericDesiredAccess, int fileAttributesAndFlags, int _shareAccess, int _creationDisposition, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        DesiredAccess desiredAccess = new DesiredAccess(genericDesiredAccess);
        FileAttributes fileAttributes = new FileAttributes(fileAttributesAndFlags);
        ShareAccess shareAccess = new ShareAccess(_shareAccess);
        CreationDisposition creationDisposition = new CreationDisposition(_creationDisposition);
        if (ROOT.equals(fileName)) {
            if (creationDisposition.isOpenExisting()) {
                return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), true);
            } else {
                return new CreateFileResult(NtStatus.STATUS_NOT_A_DIRECTORY.getStatus(), false);
            }
        } else {
            if (dokanFileInfo.isDirectory()) {
                if (creationDisposition.isCreateNew() || creationDisposition.isOpenAlways()) {
                    try {
                        fileSystem.createDirectory(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), true);
                    } catch (Exception e) {
                        return new CreateFileResult(NtStatus.STATUS_OBJECT_NAME_COLLISION.getStatus(), false);
                    }
                }
            } else {
                if (creationDisposition.isCreateNew()) {
                    try {
                        FileInfo fileInfo = fileSystem.listFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_OBJECT_NAME_COLLISION.getStatus(), fileInfo.isDirectory());
                    } catch (Exception e) {
                    }
                    try {
                        fileSystem.createRegularFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), false);
                    } catch (Exception e) {
                    }
                } else if (creationDisposition.isOpenAlways()) {
                    try {
                        FileInfo fileInfo = fileSystem.listFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), fileInfo.isDirectory());
                    } catch (Exception e) {
                    }
                    try {
                        fileSystem.createRegularFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), false);
                    } catch (Exception e) {
                    }
                } else if (creationDisposition.isCreateAlways()) {
                    try {
                        FileInfo fileInfo = fileSystem.listFile(fileName);
                        if (!fileInfo.isDirectory())
                            fileSystem.setFileSize(fileName, 0L);
                        return new CreateFileResult(NtStatus.STATUS_OBJECT_NAME_COLLISION.getStatus(), fileInfo.isDirectory());
                    } catch (Exception e) {
                    }
                    try {
                        fileSystem.createRegularFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), false);
                    } catch (Exception e) {
                    }
                } else if (creationDisposition.isOpenExisting()) {
                    try {
                        FileInfo fileInfo = fileSystem.listFile(fileName);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), fileInfo.isDirectory());
                    } catch (Exception e) {
                        return new CreateFileResult(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getStatus(), false);
                    }
                } else if (creationDisposition.isTruncateExisting()) {
                    try {
                        FileInfo fileInfo = fileSystem.listFile(fileName);
                        if (!fileInfo.isDirectory())
                            fileSystem.setFileSize(fileName, 0L);
                        return new CreateFileResult(NtStatus.STATUS_SUCCESS.getStatus(), fileInfo.isDirectory());
                    } catch (Exception e) {
                        return new CreateFileResult(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getStatus(), false);
                    }
                }
            }
        }
        return new CreateFileResult(NtStatus.STATUS_ACCESS_DENIED.getStatus(), false);
    }

    @Override
    public void closeFile(String fileName, DokanFileInfo dokanFileInfo) {
    }

    @Override
    public DeleteDirectoryResult deleteDirectory(String fileName, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            if(!fileSystem.isDirectoryEmpty(fileName)) {
                return new DeleteDirectoryResult(NtStatus.STATUS_DIRECTORY_NOT_EMPTY.getStatus());
            }
        } catch (Exception e) {
        }
        return new DeleteDirectoryResult(NtStatus.STATUS_SUCCESS.getStatus());
    }

    @Override
    public DeleteFileResult deleteFile(String fileName, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            FileInfo fileInfo = fileSystem.listFile(fileName);
            if (fileInfo.isDirectory()) {
                boolean isDirectoryEmpty = fileSystem.isDirectoryEmpty(fileName);
                if (!isDirectoryEmpty)
                    return new DeleteFileResult(NtStatus.STATUS_DIRECTORY_NOT_EMPTY.getStatus());
            }
            return new DeleteFileResult(NtStatus.STATUS_SUCCESS.getStatus());
        } catch (Exception e) {
            return new DeleteFileResult(NtStatus.STATUS_ACCESS_DENIED.getStatus());
        }
    }

    @Override
    public void cleanup(String fileName, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        if (dokanFileInfo.deleteOnClose()) {
            try {
                fileSystem.deleteFile(fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ReadFileResult readFile(String fileName, ByteBuffer buffer, long offset, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            int readTotal = 0;
            while (buffer.hasRemaining()) {
                int read = fileSystem.readFile(fileName, buffer, offset + readTotal);
                if (read == -1) {
                    break;
                }
                readTotal += read;
            }
            return new ReadFileResult(NtStatus.STATUS_SUCCESS.getStatus(), readTotal);
        } catch (Exception e) {
            return new ReadFileResult(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getStatus(), 0);
        }
    }

    @Override
    public WriteFileResult writeFile(String fileName, ByteBuffer buffer, long offset, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            if (dokanFileInfo.writeToEndOfFile())
                offset = fileSystem.listFile(fileName).size();
            int writeTotal = 0;
            while (buffer.hasRemaining()) {
                writeTotal += fileSystem.writeFile(fileName, buffer, offset + writeTotal);
            }
            return new WriteFileResult(NtStatus.STATUS_SUCCESS.getStatus(), writeTotal);
        } catch (Exception e) {
            return new WriteFileResult(NtStatus.STATUS_ACCESS_DENIED.getStatus(), 0);
        }
    }

    @Override
    public FlushFileBuffers flushFileBuffers(String fileName, DokanFileInfo dokanFileInfo) {
        return new FlushFileBuffers(NtStatus.STATUS_SUCCESS.getStatus());
    }

    @Override
    public FindFilesResult findFiles(String fileName, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            List<FileInfo> fileInfos = fileSystem.listDirectory(fileName);
            Iterator<FindData> iterator = fileInfos.stream().map(fileInfo -> new FindData(fileInfo.filename(), FileAttribute.FILE_ATTRIBUTE_NORMAL.mask() | (fileInfo.isDirectory() ? FileAttribute.FILE_ATTRIBUTE_DIRECTORY.mask() : 0),
                    fileInfo.ctime(), fileInfo.atime(), fileInfo.mtime(),
                    fileInfo.size())).toList().iterator();
            return new FindFilesResult(NtStatus.STATUS_SUCCESS.getStatus(), iterator);
        } catch (Exception e) {
            return new FindFilesResult(NtStatus.STATUS_ACCESS_DENIED.getStatus(), Collections.emptyIterator());
        }
    }

    @Override
    public GetFileInformationResult getFileInformation(String fileName, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            FileInfo fileInfo = fileSystem.listFile(fileName);
            return new GetFileInformationResult(NtStatus.STATUS_SUCCESS.getStatus(), FileAttribute.FILE_ATTRIBUTE_NORMAL.mask() | (fileInfo.isDirectory() ? FileAttribute.FILE_ATTRIBUTE_DIRECTORY.mask() : 0), 1234567890, fileInfo.ctime(), fileInfo.atime(), fileInfo.mtime(), 0L, fileInfo.size(), 0);
        } catch (Exception e) {
        }
        return new GetFileInformationResult(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getStatus(), 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public GetFileSecurityResult getFileSecurity(String fileName, int securityInformation, ByteBuffer security, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            FileInfo fileInfo = fileSystem.listFile(fileName);
            String s = fileInfo.isDirectory() ? "D:PAI(A;OICI;FA;;;WD)" : "D:AI(A;ID;FA;;;WD)";
            byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
            ByteBuffer src = ByteBuffer.wrap(bytes);
            while (src.hasRemaining() && security.hasRemaining()) {
                security.put(src.get());
            }
            return new GetFileSecurityResult(NtStatus.STATUS_SUCCESS.getStatus(), bytes.length);
        } catch (Exception e) {
        }
        return new GetFileSecurityResult(NtStatus.STATUS_ACCESS_DENIED.getStatus(), 0);
    }

    @Override
    public SetFileAttributesResult setFileAttributes(String fileName, int fileAttributes, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        return new SetFileAttributesResult(NtStatus.STATUS_SUCCESS.getStatus());
    }

    @Override
    public SetFileTimeResult setFileTime(String fileName, long creationTime, long lastAccessTime, long lastWriteTime, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        return new SetFileTimeResult(NtStatus.STATUS_SUCCESS.getStatus());
    }

    @Override
    public MoveFileResult moveFile(String fileName, String newFileName, boolean replaceIfExisting, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        newFileName = newFileName.replace('\\', '/');
        try {
            fileSystem.renameFile(fileName, newFileName);
            return new MoveFileResult(NtStatus.STATUS_SUCCESS.getStatus());
        } catch (Exception e) {
            return new MoveFileResult(NtStatus.STATUS_ACCESS_DENIED.getStatus());
        }
    }

    @Override
    public SetEndOfFileResult setEndOfFile(String fileName, long offset, DokanFileInfo dokanFileInfo) {
        fileName = fileName.replace('\\', '/');
        try {
            fileSystem.setFileSize(fileName, offset);
            return new SetEndOfFileResult(NtStatus.STATUS_SUCCESS.getStatus());
        } catch (Exception e) {
            return new SetEndOfFileResult(NtStatus.STATUS_ACCESS_DENIED.getStatus());
        }
    }

    @Override
    public SetAllocationSizeResult setAllocationSize(String fileName, long allocSize, DokanFileInfo dokanFileInfo) {
        return new SetAllocationSizeResult(NtStatus.STATUS_SUCCESS.getStatus());
    }

    @Override
    public GetDiskFreeSpaceResult getDiskFreeSpace(DokanFileInfo dokanFileInfo) {
        return new GetDiskFreeSpaceResult(NtStatus.STATUS_SUCCESS.getStatus(), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Override
    public GetVolumeInformationResult getVolumeInformation(DokanFileInfo dokanFileInfo) {
        return new GetVolumeInformationResult(NtStatus.STATUS_SUCCESS.getStatus(), "dokany4j", 1234567890, 110, SUPPORTED_FLAGS, "dokan4j");
    }

    @Override
    public MountedResult mounted(String mountPoint, DokanFileInfo dokanFileInfo) {
        return new MountedResult(NtStatus.STATUS_SUCCESS.getStatus());
    }

    enum FileAttribute {

        FILE_ATTRIBUTE_ARCHIVE(0x20),
        FILE_ATTRIBUTE_COMPRESSED(0x800),
        FILE_ATTRIBUTE_DEVICE(0x40),
        FILE_ATTRIBUTE_DIRECTORY(0x10),
        FILE_ATTRIBUTE_ENCRYPTED(0x4000),
        FILE_ATTRIBUTE_HIDDEN(0x2),
        FILE_ATTRIBUTE_INTEGRITY_STREAM(0x8000),
        FILE_ATTRIBUTE_NORMAL(0x80),
        FILE_ATTRIBUTE_NOT_CONTENT_INDEXED(0x2000),
        FILE_ATTRIBUTE_NO_SCRUB_DATA(0x20000),
        FILE_ATTRIBUTE_OFFLINE(0x1000),
        FILE_ATTRIBUTE_READONLY(0x1),
        FILE_ATTRIBUTE_REPARSE_POINT(0x400),
        FILE_ATTRIBUTE_SPARSE_FILE(0x200),
        FILE_ATTRIBUTE_SYSTEM(0x4),
        FILE_ATTRIBUTE_TEMPORARY(0x100),
        FILE_ATTRIBUTE_VIRTUAL(0x10000);

        private final int value;

        private FileAttribute(int value) {
            this.value = value;
        }

        public int mask() {
            return value;
        }
    }

    public static final int FILE_CASE_SENSITIVE_SEARCH = 0x00000001;
    public static final int FILE_CASE_PRESERVED_NAMES = 0x00000002;
    public static final int FILE_UNICODE_ON_DISK = 0x00000004;
    public static final int SUPPORTED_FLAGS = FILE_CASE_SENSITIVE_SEARCH | FILE_CASE_PRESERVED_NAMES | FILE_UNICODE_ON_DISK;

}
