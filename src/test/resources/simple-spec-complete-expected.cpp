#include "rvmonitor.h"

using namespace std;
namespace rv
{
    // Declarations of shared variables


    namespace monitor
    {
        std::set<std::string> monitorTopics;
        std::set<std::string> allMonitors;
        std::set<std::string> enabledMonitors;
        std::map<std::string,std::string> topicsAndTypes;

        void initMonitorTopics()
        {
            monitorTopics.insert("/test");
            topicsAndTypes["/test"] = "std_msgs/Float64";

            allMonitors.insert("TestMonitor");

        }

        void initAdvertiseOptions(std::string topic, ros::AdvertiseOptions &ops_pub)
        {
            if (topic == "/test") {
                ops_pub.init<std_msgs::Float64>(topic, 1000);
            }
        }

    }

    RVMonitor::RVMonitor(string topic, ros::SubscribeOptions &ops_sub)
    {
        topic_name = topic;
        server_manager = rv::ServerManager::instance();

        if (topic == "/test") {
            ops_sub.init<std_msgs::Float64>(topic, 1000, boost::bind(&RVMonitor::monitorCallback_testEvent, this, _1));
        }
    }

    void RVMonitor::monitorCallback_testEvent(const std_msgs::Float64::ConstPtr& monitored_msg)
    {

        std_msgs::Float64 rv_msg;
        rv_msg.data = monitored_msg->data;


        double& testPar = rv_msg.data;

        if(monitor::enabledMonitors.find("TestMonitor") != monitor::enabledMonitors.end())
        {
           ROS_INFO("Test Parameter Binding is %g", testPar);
       }


        ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(rv_msg);
        server_manager->publish(topic_name, serializedMsg);
    }


}
