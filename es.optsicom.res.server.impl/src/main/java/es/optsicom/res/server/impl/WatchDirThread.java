package es.optsicom.res.server.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

	
public class WatchDirThread extends Thread {
		
		String dir;
		boolean executionFinished = false;
		WatchDir watchDir;
				
		public void run(){
			
			try {
				watchDir = new WatchDir(Paths.get(dir), true);
				while(!executionFinished){
					watchDir.processEvents();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void setDir(String dir) {
			this.dir = dir;
		}
		
		public void setExecutionFinished(boolean executionFinished) {
			this.executionFinished = executionFinished;
		}
		
		public void closeWatchDir() throws IOException{
			this.watchDir.closeWatchService();
		}
		
		public List<File> getResultFiles() {
			return this.watchDir.getResultFiles();
		}

}
