package es.optsicom.res.server.impl;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WatchDir {

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final boolean recursive;
	private boolean trace = false;
	private List<File> resultFiles; 
	
	
	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	private void registerAll(final Path start) throws IOException {
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	WatchDir(Path dir, boolean recursive) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;
		this.resultFiles = new ArrayList<File>();

		if (recursive) {
			System.out.format("Scanning %s ...\n", dir);
			registerAll(dir);
			System.out.println("Done.");
		} else {
			register(dir);
		}

		this.trace = true;
	}

	void processEvents() {
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
			} else {

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind kind = event.kind();

					WatchEvent<Path> ev = cast(event);
					Path name = ev.context();
					Path child = dir.resolve(name);

					if (recursive && (kind == ENTRY_CREATE)) {
						try {
							if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
								registerAll(child);
							} else {
								if(!resultFiles.contains(child.toFile())){
									resultFiles.add(child.toFile());
								}
							}
						} catch (IOException x) {
							
						}
					}

					if (recursive && (kind == ENTRY_DELETE)) {
						if (!Files.isDirectory(child, NOFOLLOW_LINKS)) {
							if(resultFiles.contains(child.toFile())){
								resultFiles.remove(child.toFile());
							}
						}
					}
				}

				boolean valid = key.reset();
				if (!valid) {
					keys.remove(key);
				}
			}
		
	}

	public List<File> getResultFiles() {
		return this.resultFiles;
	}
	
	public void closeWatchService() throws IOException {
		watcher.close();
	}
}