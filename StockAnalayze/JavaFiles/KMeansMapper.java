package solution;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


// first iteration, k-random centers, in every follow-up iteration we have new calculated centers
public class KMeansMapper extends
		Mapper<LongWritable, Text, ClusterCenter, Vector> {

	List<ClusterCenter> centers = new LinkedList<ClusterCenter>();
	private static final Log LOG = LogFactory.getLog(KMeansMapper.class);
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		Configuration conf = context.getConfiguration();
		Path centroids = new Path(conf.get("centroid.path"));
		FileSystem fs = FileSystem.get(conf);

		SequenceFile.Reader reader = new SequenceFile.Reader(fs, centroids, conf);
		ClusterCenter key = new ClusterCenter();
		IntWritable value = new IntWritable();
		
		while (reader.next(key, value)) {
			LOG.info(key.getCenter() + " " + key.toString());
			centers.add(new ClusterCenter(key));
		}
		
		reader.close();
	}

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		Vector vector = new Vector(value);
		ClusterCenter nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (ClusterCenter c : centers) {
			double dist = DistanceMeasurer.measureDistance(c, vector);
			if (nearest == null || nearestDistance > dist) {
				nearest = c;
				nearestDistance = dist;
			} 
		}
		
		context.write(nearest, vector);
	}
}
