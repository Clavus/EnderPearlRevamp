package clavus.enderpearlrevamp.runnable;

import java.util.HashMap;

public class ParamRunnable implements Runnable 
{
	private HashMap<String, Object> parameters;
	
	public ParamRunnable(HashMap<String, Object> params)
	{
		this.parameters = params;
	}
	
	protected void setParam(String key, Object val)
	{
		this.parameters.put(key, val);
	}
	
	protected Object getParam(String key)
	{
		if (parameters == null) { return null; }
		return parameters.get(key);
	}
	
	protected HashMap<String, Object> getParams()
	{
		return parameters;
	}

	@Override
	public void run() {
		
	}
	
}
