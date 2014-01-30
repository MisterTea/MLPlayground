package com.github.mistertea.crunch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

public class ThriftCollectionOutputFormat<VT extends TBase> extends
		TextOutputFormat<NullWritable, Writable> {
	public static class ThriftOutputFormatWriterWrapper<VT extends TBase>
			extends RecordWriter<NullWritable, Writable> {

		private RecordWriter<NullWritable, Writable> writer;
		private Class<VT> thriftClazz;
		private List<Integer> fields;

		public ThriftOutputFormatWriterWrapper(Class<VT> thriftClazz,
				List<Integer> fields,
				RecordWriter<NullWritable, Writable> recordWriter) {
			this.thriftClazz = thriftClazz;
			this.fields = new ArrayList<Integer>(fields);
			this.writer = recordWriter;
		}

		private TDeserializer deserializer = new TDeserializer(
				new TBinaryProtocol.Factory());

		@SuppressWarnings("unchecked")
		public void write(NullWritable arg0, Writable arg1) throws IOException {
			//System.out.println("NULL KEY " + arg0);
			// System.out.println("VALUE " + arg1);
			try {
				VT thriftObject = thriftClazz.newInstance();
				BytesWritable arg1Bytes = (BytesWritable) arg1;
				deserializer.deserialize(thriftObject, arg1Bytes.getBytes());
				StringBuffer sb = new StringBuffer();
				for (Integer i : fields) {
					if (sb.length() > 0) {
						sb.append("\t");
					}
					sb.append(thriftObject.getFieldValue(
							thriftObject.fieldForId(i)).toString().replaceAll("\t", " "));
				}
				writer.write(NullWritable.get(),
						new Text(sb.toString()));
				//System.out.println("DESERIALIZED VALUE " + sb.toString());
			} catch (TException e) {
				throw new IOException(e);
			} catch (InterruptedException e) {
				throw new IOException(e);
			} catch (InstantiationException e) {
				throw new IOException(e);
			} catch (IllegalAccessException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void close(TaskAttemptContext arg0) throws IOException,
				InterruptedException {
			writer.close(arg0);
		}
	}

	private Class<VT> thriftClazz;
	private ArrayList<Integer> fields;

	public ThriftCollectionOutputFormat(Class<VT> thriftClazz,
			List<Integer> fields) {
		super();
		this.thriftClazz = thriftClazz;
		this.fields = new ArrayList<Integer>(fields);
	}

	@Override
	public RecordWriter<NullWritable, Writable> getRecordWriter(
			TaskAttemptContext job) throws IOException, InterruptedException {
		return new ThriftOutputFormatWriterWrapper<VT>(thriftClazz, fields,
				super.getRecordWriter(job));
	}
}
