package org.goobs.stanford;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.logging.PrettyLoggable;
import edu.stanford.nlp.util.logging.Redwood;
import org.goobs.testing.Dataset;
import org.goobs.util.Range;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Arrays;

public class SerializedCoreMapDataset extends Dataset<CoreMapDatum> implements Serializable, PrettyLoggable {
	private static final long serialVersionUID = 1L;

	private String file;

	private boolean isPiecewise = false;
	private CoreMapDatum[] maps;
	private WeakReference<CoreMapDatum>[] weakMaps;
	private File[] files;

	public <E extends CoreMap> SerializedCoreMapDataset(String file, E[] maps, boolean piecewise){
		//(create dataset)
		this.file = file;
		this.maps = new CoreMapDatum[maps.length];
		for(int i=0; i<maps.length; i++){
			this.maps[i] = new CoreMapDatum(maps[i],i);
		}
		this.isPiecewise = piecewise;
		if(this.isPiecewise){
			new File(file).mkdirs();
		}
		//(save dataset)
		save();
	}

	public <E extends CoreMap> SerializedCoreMapDataset(String file, E[] maps){
		this(file, maps, false);
	}

	@SuppressWarnings({"unchecked"})
	public SerializedCoreMapDataset(String file){
		this.file = file;
		if(new File(file).isDirectory()){
			this.isPiecewise = true;
			//(get files)
			this.files = new File(file).listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.length() > 0;
				}
			});
			Arrays.sort(files);
			//(load files)
			this.weakMaps = (WeakReference<CoreMapDatum>[]) new WeakReference[files.length];
			for(int i=0; i<files.length; i++){
				this.weakMaps[i] = new WeakReference<CoreMapDatum>( null );
			}
		} else {
			this.isPiecewise = false;
			SerializedCoreMapDataset term = readObject(this.file);
			this.file = term.file;
			this.maps = term.maps;
		}
	}


	public void saveAs(String path, boolean isPiecewise){
		//(load all elements -- store to prevent WeakReference from decaching)
		if(isPiecewise) {
			if(!new File(path).isDirectory()){
				throw new IllegalStateException("Cannot save piecewise dataset to a regular file");
			}
			//(write each datum)
			this.files = new File[this.size()];
			for(int i=0; i<size(); i++){
				writeObject(path+"/"+i+".ser.gz", this.get(i));
				files[i] = new File(path+"/"+i+".ser.gz");
			}
		} else {
			if(new File(path).isDirectory()){
				throw new IllegalStateException("Cannot save dataset file to a directory");
			}
			writeObject(path, this);
		}
		//(update variables)
		this.file = path;
		this.isPiecewise = isPiecewise;
	}
	
	public void saveAs(String path){
		saveAs(path,this.isPiecewise);
	}
	
	public void save(){
		saveAs(this.file, this.isPiecewise);
	}
	
	public void chdir(String newDir){
		this.file = newDir;
		if(isPiecewise){
			new File(newDir).mkdirs();
		}
	}

	public void saveDatum(int index){
		if(!this.isPiecewise){
			throw new IllegalStateException("Cannot save single datum in non-piecewise mode");
		}
		writeObject(files[index].getAbsolutePath(), get(index));
	}

	@Override public int numExamples() {
		if(maps != null){
			return maps.length;
		} else if(weakMaps != null){
			return weakMaps.length;
		} else {
			throw new IllegalStateException("Cannot determine size of SerializedCoreMapDataset");
		}
	}

	@Override public CoreMapDatum get(int id) {
		if(maps != null){
			return maps[id];
		} else {
			if(!isPiecewise){
				throw new IllegalStateException("maps are null, but dataset is not piecewise");
			}
			WeakReference<CoreMapDatum> ref = weakMaps[id];
			CoreMapDatum rtn = ref.get();
			if(rtn == null){
				CoreMap impl = readObject(files[id].getPath());
				rtn = new CoreMapDatum(impl, id);
				weakMaps[id] = new WeakReference<CoreMapDatum>(rtn);
			}
			return rtn;
		}
	}

	@Override
	public Range range() { return new Range(0,numExamples()); }

	@Override
	public void prettyLog(Redwood.RedwoodChannels redwoodChannels, String description) {
		Redwood.startTrack(description);
		for(int i=0; i<this.size(); i++){
			redwoodChannels.prettyLog("Datum " + i, this.get(i));
		}
		Redwood.endTrack(description);
	}


	@SuppressWarnings({"unchecked"})
	private static <T> T readObject(String file){
		try{
			return (T) IOUtils.readObjectFromFile(file);
//			FileInputStream fos = new FileInputStream(file);
//			ObjectInputStream out = new ObjectInputStream(fos);
//			T term = (T) out.readObject();
//			out.close();
//			return term;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> void writeObject(String file, T object){
		try{
			IOUtils.writeObjectToFile(object, file);
//			FileOutputStream fos = new FileOutputStream(file);
//			ObjectOutputStream out = new ObjectOutputStream(fos);
//			out.writeObject(object);
//			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
