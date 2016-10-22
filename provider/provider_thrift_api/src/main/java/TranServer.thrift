namespace java org.shoper.schedule.face
service TransServer{
	string sendTask(1:string task);
	string getStatus();
	list<string> getAllRunning();
	i32 kill(1:string id);
	bool isAvailable();
}