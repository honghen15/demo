package cn.howardliu.demo.storm.log;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <br/>create at 16-1-18
 *
 * @author liuxh
 * @since 1.0.0
 */
public class KafkaTopology3 {
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopology3.class);

    public static void main(String[] args) throws Exception {
        // 配置Zookeeper地址
        BrokerHosts brokerHosts = new ZkHosts("zk1:2181,zk2:2281,zk3:2381");
        // 配置Kafka订阅的Topic，以及zookeeper中数据节点目录和名字
        SpoutConfig spoutConfig = new SpoutConfig(brokerHosts, "topic1", "/zkkafkaspout", "kafkaspout");
        // 配置KafkaBolt中的kafka.broker.properties
        Config conf = new Config();
        Properties props = new Properties();
        // 配置Kafka broker地址
        props.put("metadata.broker.list", "dev2_55.wfj-search:9092");
        // serializer.class为消息的序列化类
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        conf.put("kafka.broker.properties", props);
        // 配置KafkaBolt生成的topic
        conf.put("topic", "topic2");
        spoutConfig.scheme = new SchemeAsMultiScheme(new MessageScheme());
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("spout", new KafkaSpout(spoutConfig));
        builder.setBolt("bolt", new SentenceBolt()).shuffleGrouping("spout");
        builder.setBolt("kafkabolt", new KafkaBolt<String, Integer>()).shuffleGrouping("bolt");
        if (args.length == 0) {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("Topo", conf, builder.createTopology());
            Utils.sleep(100000);
            cluster.killTopology("Topo");
            cluster.shutdown();
        } else {
            conf.setNumWorkers(3);
            StormSubmitter.submitTopology(args[0], conf, builder.createTopology());
        }
    }

    public static class SentenceBolt extends BaseBasicBolt {
        @Override
        public void execute(Tuple input, BasicOutputCollector collector) {
            String word = (String) input.getValue(0);
            String out = "I'm " + word + "!";
            System.out.println("out=" + out);
            collector.emit(new Values(out));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("message"));
        }
    }

    public static class MessageScheme implements Scheme {
        @Override
        public List<Object> deserialize(byte[] ser) {
            try {
                String msg = new String(ser, "UTF-8");
                return new Values(msg);
            } catch (UnsupportedEncodingException ignored) {
            }
            return null;
        }

        @Override
        public Fields getOutputFields() {
            return new Fields("msg");
        }
    }
}