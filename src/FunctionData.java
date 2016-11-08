

import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

class FunctionData
{
	public int []raw;
	int width,height;
	// przedzial x<0,1>
	public double getFunctionValue(double x)
	{
		if (x>1)
			x=1;
		if (x<0)
			x=0;
		int i=(int)Math.floor(x*(width-1));
		int j=(int)Math.ceil(x*(width-1));
		double alpha=(x-i);
		double fi=((double)raw[i])/height;
		double fj=((double)raw[j])/height;
		return 1-(fi*alpha+fj*(1-alpha));
	}
	public Object clone()
	{
		FunctionData d=new FunctionData();
		d.raw=new int[raw.length];
		for (int i=0;i<raw.length;i++)
		{
			d.raw[i]=raw[i];
		}
		d.width=width;
		d.height=height;
		return d;
	}
};
class FunctionGraphGadget extends Canvas
{
	private static final long serialVersionUID = 2465825594632819900L;
	Point lastpoint;
	FunctionData dat;
	FunctionData datorig;
	
	public void updateRaw(Point p)
	{
		if (p.x<0 || p.x>=this.getWidth() || 
				p.y<0 || p.y>=this.getHeight())
			return; 
		dat.raw[p.x]=p.y;
		if (!(lastpoint.x<0 || lastpoint.x>=this.getWidth() || 
				lastpoint.y<0 || lastpoint.y>=this.getHeight()))
		{
			int x1,x2,y1,y2;
			if (p.x<lastpoint.x)
			{
				x1=p.x;
				y1=p.y;
				x2=lastpoint.x;
				y2=lastpoint.y;
			}
			else
			{
				x1=lastpoint.x;
				y1=lastpoint.y;
				x2=p.x;
				y2=p.y;
			}
			int diff=y2-y1;
			int dx=x2-x1;
			double step=((double)diff)/dx;
			double val=y1;
			for (int i=x1;i<=x2;i++)
			{
				dat.raw[i]=(int)val;
				val+=step;
			}
			
			for(int i=(x1-20<1?1:x1-20);i<(x2+20>dat.width-1?dat.width-1:x2+20);i++)
				dat.raw[i]=(dat.raw[i-1]+dat.raw[i+1])/2;
		}
		this.repaint();
	}
	// funckja do obsługi skrótów
	public FunctionGraphGadget()
	{
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e)
			{
				Point p=e.getPoint();
				lastpoint=p;
				updateRaw(p);
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseDragged(MouseEvent e)
			{
				Point p=e.getPoint();
				updateRaw(p);
				lastpoint=p;
			}
		});
		dat=new FunctionData();
	}

	public void paint(Graphics g)
	{
		dat.width=this.getWidth();
		dat.height=this.getHeight();
		g.setColor(Color.black);
		g.drawRect(0,0,dat.width-1,dat.height-1);
		g.setColor(Color.blue);
		int [] arr= new int[dat.width];
		for(int i=0;i<dat.width;i++ )
			arr[i]=i;
		g.drawPolyline(arr,dat.raw,dat.width);
	}

	public void setBounds(Rectangle r)
	{
		setBounds(r.x,r.y,r.width,r.height);
	}

	public void setBounds(int x, int y, int width, int height)
	{
		super.setBounds(x,y,width,height);
		dat.width=width;
		dat.height=height;
		dat.raw=new int[dat.width];
		for (int i=0;i<dat.width;i++)
			dat.raw[i]=dat.width-i;
		datorig=dat;
	}

	// FUNKCJE POJAZDOW
	public void fillLinear()
	{
		for (int i=0;i<dat.width;i++)
			dat.raw[i]=dat.width-i;
		repaint();
	}
	public void fillInverse()
	{
		for (int i=0;i<dat.width;i++)
			dat.raw[i]=i;
		repaint();
	}
	public void fillConstant()
	{
		for (int i=0;i<dat.width;i++)
			dat.raw[i]=dat.height/2;		
		repaint();
	}
	public void fillHill()
	{
		for (int i=0;i<dat.width;i++)
		{
			dat.raw[i]=(i-dat.width/2)*(i-dat.width/2)/dat.width+dat.width/2;
			if (dat.raw[i]>dat.height-1)
				dat.raw[i]=dat.height-1;
			dat.raw[i]=dat.raw[i];
		}		
		repaint();
	}
}


class ArenaGadget extends Canvas
{
	Rectangle offDimension;
	Image offImage;
	Graphics offGraphics;

	Point[] lights;
	int nlights;
	
	Image lightImage;
	Graphics lightGraphics;
	Rectangle lightDimension;
	double [][] lightMap;
	int LightMapScale;
	int [] colors;
	double min,max; 
	double [][] oldValue;
	int draggedLight; 
	int operation; 
	int chosenVehicle;
	int chosenLight;
	
	boolean robotsEmitLight;
	boolean drawTrajectory;
	boolean drawTrail;
	boolean perimeter;
	int trajectoryLength;
	//do dodatkowych kolorow swiatla, miedzy innymi do modelu 4.b
	int colorOp;
	double MaxIntensity;
	double MinDistance;
	Vector vehiclelist;

	public ArenaGadget()
	{
		this.offGraphics=null;
		this.lightGraphics=null;
		lights=new Point[50]; // ograniczenie ilosci swiatel 
		nlights=1; // minimalna ilosc swiatla 
		LightMapScale=4;
		draggedLight=-1;
		operation=0;
		chosenVehicle=-1;
		chosenLight=-1;
		trajectoryLength=1000; 
		colorOp=1; //kolor swiatla 
		//luminacja swiatla (czyzby maslo maslane?)
		// zasieg swiatla 
		MaxIntensity=1000;
		MinDistance=100;
		robotsEmitLight=false;
		drawTrajectory=false;
		drawTrail=false;
		vehiclelist=new Vector();

		this.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e)
			{	
				if ((((int)e.getModifiers())&((int)InputEvent.BUTTON1_MASK))>0)
				{
					// lpm wybiera pojazd 
					Point p=e.getPoint();
					for (int i=0;i<vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) vehiclelist.get(i);
						if (Math.abs(p.x-bv.pos.x)<bv.width/2 && Math.abs(p.y-bv.pos.y)<bv.height/2)
						{
							chosenVehicle=i;
							repaint();  
							return;
						}
					}
					//lpm wybiera swiatlo 
					for (int i=0;i<nlights;i++)
					{
						if (Math.abs(p.x-lights[i].x)<20 && Math.abs(p.y-lights[i].y)<20)
						{
							chosenLight=i;
							repaint();
							return;
						}
					}
					// ctrl + lpm dodaj swiatlo 
					
					if (nlights<lights.length-1 && e.isControlDown())
					{
						lights[nlights++]=p;
						updateLightGraph();
						for(int i=0;drawTrajectory && i<vehiclelist.size();i++)
						{
							Vehicle bv=(Vehicle) vehiclelist.get(i);
							bv.calculateTrajectory(trajectoryLength);
							repaint();
						}
						return;
					}
					// shift + lpm dodaj pojazd
					if (e.isShiftDown())
					{
						VehicleModel m=((Braitenberg)getParent()).vehicleConstructor.getVehicleModel();
						FunctionData [] f=new FunctionData[2];
						f[0]=(FunctionData)(((Braitenberg)getParent()).funcL.dat.clone());
						f[1]=(FunctionData)(((Braitenberg)getParent()).funcR.dat.clone());
						Vehicle vehicle=new Vehicle(e.getPoint(),((Braitenberg)getParent()).arena,m,f,((Braitenberg)getParent()).rnd.nextInt(360));
						((Braitenberg)getParent()).arena.vehiclelist.add(vehicle);
						setCursor(new Cursor(Cursor.WAIT_CURSOR));
						if(((Braitenberg)getParent()).arena.drawTrajectory) vehicle.calculateTrajectory(((Braitenberg)getParent()).arena.trajectoryLength);
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
						if(((Braitenberg)getParent()).arena.robotsEmitLight)((Braitenberg)getParent()).arena.updateLightGraph();
						((Braitenberg)getParent()).arena.repaint();
						return;
					}
				}
				if ((((int)e.getModifiers())&((int)InputEvent.BUTTON3_MASK))>0)
				{
					Point p=e.getPoint();
					// ctrl + ppm - usun swiatlo
					for (int i=0;i<nlights;i++)
					{
						if (Math.abs(p.x-lights[i].x)<15 && Math.abs(p.y-lights[i].y)<15 && e.isControlDown() && nlights>1)
						{
							if(i==chosenLight)
								chosenLight=-1;
							lights[i]=lights[nlights-1];
							nlights--;
							updateLightGraph();
							
							for(int j=0;drawTrajectory && j<vehiclelist.size();j++)
							{
								Vehicle bv=(Vehicle) vehiclelist.get(j);
								bv.calculateTrajectory(trajectoryLength);
								repaint();
							}
							return;
						}
					}
					//shift+ppm usun pojazd
					for (int i=0;i<vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) vehiclelist.get(i);
						if (Math.abs(p.x-bv.pos.x)<bv.width/2 && Math.abs(p.y-bv.pos.y)<bv.height/2 && e.isShiftDown())
						{
							if(chosenVehicle==i)
								chosenVehicle=-1;

								setCursor(new Cursor(Cursor.WAIT_CURSOR));
								vehiclelist.remove(bv);
								chosenVehicle=-1;
								if(robotsEmitLight) updateLightGraph();
								setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
								for(int j=0;drawTrajectory && robotsEmitLight && j<vehiclelist.size();j++)
								{
									Vehicle bv2=(Vehicle) vehiclelist.get(j);
									bv2.calculateTrajectory(trajectoryLength);
									repaint();
								}
							repaint();  
							return;
						}
					}
				}
			}
			public void mousePressed(MouseEvent e)
			{
				Point p=e.getPoint();
				chosenVehicle=-1;
				chosenLight=-1;
				if ((((int)e.getModifiers())&((int)InputEvent.BUTTON1_MASK))>0)
				{	
					// PRZESUNIECIE pojazdu
					for (int i=0;i<vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) vehiclelist.get(i);
						if (Math.abs(p.x-bv.pos.x)<bv.width/2 && Math.abs(p.y-bv.pos.y)<bv.height/2)
						{
							operation=1;
							chosenVehicle=i;
							bv.trailLen=0;
							bv.trailLim=0;
							updateParent(bv);
							return;
						}
					}
					//przesuwanie swiatla wybranego
					for (int i=0;i<nlights;i++)
					{
						if (Math.abs(p.x-lights[i].x)<20 && Math.abs(p.y-lights[i].y)<20)
						{
							draggedLight=i;
							chosenLight=i;
							return;
						}
					}
				}
				//PPM wybor + obrot // ewentualnie do wyrzucenia w celu optymalizacji
				
				if ((((int)e.getModifiers())&((int)InputEvent.BUTTON3_MASK))>0)
				{
					for (int i=0;i<vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) vehiclelist.get(i);
						if (Math.abs(p.x-bv.pos.x)<bv.width/2 && Math.abs(p.y-bv.pos.y)<bv.height/2)
						{
							operation=2;
							chosenVehicle=i;
							updateParent((Vehicle)vehiclelist.get(i));
							return;
						}
					}					
				}
			}
			public void mouseReleased(MouseEvent e)
			{
				if (draggedLight>=0)
				{
					draggedLight=-1;
					updateLightGraph();
					if(drawTrajectory)
						for(int i=0;drawTrajectory && i<vehiclelist.size();i++)
						{
							Vehicle bv=(Vehicle) vehiclelist.get(i);
							bv.calculateTrajectory(trajectoryLength);
							repaint();
						}
				}
				operation=0;
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseDragged(MouseEvent e)
			{
				Point p=e.getPoint();
				//przesuwanie swiatla
				if (draggedLight>=0)
				{
					lights[draggedLight]=p;
					repaint();
					updateLightGraph(); 
				}
				//przesuwanie pojazdu
				else if (operation==1 && chosenVehicle>=0)
				{
					Vehicle bv=(Vehicle) vehiclelist.get(chosenVehicle);
					bv.pos.x=p.x;
					bv.pos.y=p.y;
					bv.updateSensorPositions();
					bv.updateMotorPositions();
					setCursor(new Cursor(Cursor.WAIT_CURSOR));
					
					if(robotsEmitLight) updateLightGraph();
					if(drawTrajectory) bv.calculateTrajectory(trajectoryLength);
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					repaint();
				}
				//obrot rakiety 
				else if (operation==2 && chosenVehicle>=0)
				{
					Vehicle bv=(Vehicle) vehiclelist.get(chosenVehicle);
					Point t=new Point((int)bv.pos.x,(int)bv.pos.y);
					if (t.x!=p.x || t.y!=p.y)
					{
						bv.theta=Math.atan2(t.y-p.y,t.x-p.x)+Math.PI;
					}
					bv.updateSensorPositions();
					bv.updateMotorPositions();
					setCursor(new Cursor(Cursor.WAIT_CURSOR));
					if(drawTrajectory) bv.calculateTrajectory(trajectoryLength);
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					repaint();
				}
			}
		});
	}
	
	public void updateParent(Vehicle bv)
	{
		// aktualizacja rodzica
		Braitenberg par=(Braitenberg)getParent();
		par.funcL.dat=(FunctionData)bv.functions[0].clone();
		par.funcL.repaint();
		par.funcR.dat=(FunctionData)bv.functions[1].clone();
		par.funcR.repaint();
		//ODWROTNOSC f(x) sensors[i]=new DoubleVector(1.2*(0.5-((double)plist[i].y)/r.height),1.8*(0.5-((double)plist[i].x)/r.width));
		par.vehicleConstructor.sensors[0].setLocation((int)((0.5-bv.model.sensors[0].y/1.8)*par.vehicleConstructor.getBounds().height),(int)((0.5-bv.model.sensors[0].x/1.21)*par.vehicleConstructor.getBounds().width));
		par.vehicleConstructor.sensors[1].setLocation((int)((0.5-bv.model.sensors[1].y/1.78)*par.vehicleConstructor.getBounds().height),(int)((0.5-bv.model.sensors[1].x/1.21)*par.vehicleConstructor.getBounds().width));
		par.vehicleConstructor.repaint();
	}

	public void createLightGraph()
	{
		if (lightGraphics==null)
		{
			// set up graphics 
			lightDimension=this.getBounds();
			lightDimension.width/=LightMapScale;
			lightDimension.height/=LightMapScale;
			lightImage=new BufferedImage(lightDimension.width,lightDimension.height,BufferedImage.TYPE_INT_RGB);//createVolatileImage(this.getWidth(),this.getHeight(),new ImageCapabilities(true));
			lightGraphics=lightImage.getGraphics();
			lightMap=new double [lightDimension.width][lightDimension.height];
			lights[0]=new Point();
			lights[0].x=lightDimension.width*LightMapScale/2;
			lights[0].y=lightDimension.height*LightMapScale/2;
			colors=new int[lightDimension.width*lightDimension.height];
			nlights=1;
			updateLightGraph();
		}
	}


	public void updateLightGraph()
	{
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
//		long start=System.currentTimeMillis();
		min=1e20;
		max=-1;
		for (int i=0;i<lightDimension.width;i++)
		{
			for (int j=0;j<lightDimension.height;j++)
			{
				lightMap[i][j]=0;
				for (int k=0;k<nlights;k++)
				{
					lightMap[i][j]+=MaxIntensity/((lights[k].x-i*LightMapScale)*(lights[k].x-i*LightMapScale)+(lights[k].y-j*LightMapScale)*(lights[k].y-j*LightMapScale)+MinDistance*MinDistance);	
				}
				
				if (lightMap[i][j]>max)
					max=lightMap[i][j];
				if (lightMap[i][j]<min)
					min=lightMap[i][j];
			}
		}

		long renderStarts=System.currentTimeMillis();
		// normalizacja
		double diff=max-min;
		if (diff<1e-10)
			diff=1;
		// tworzenie grafu siatla - dodanie koloru 
		for (int i=0;i<lightDimension.width;i++)
			for (int j=0;j<lightDimension.height;j++)
			{
				lightMap[i][j]=(lightMap[i][j]-min)/diff;
				int r=(int)(lightMap[i][j]*255);
				
					colors[j*lightDimension.width+i]=r<<32 | r<<16 | r<<8;
			
			}
		BufferedImage b=(BufferedImage)lightImage;
		b.setRGB(0, 0, lightDimension.width, lightDimension.height, colors, 0, lightDimension.width);
	//	long end=System.currentTimeMillis();
	//System.out.println("S::"+nlights+" T:: "+(end-start)+"ms [ "+(end-renderStarts)+"]\n");
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		repaint();
	}

	public void paint(Graphics g) {
		update(g);
	}

	public void update(Graphics g) 
	{
		
		if ( (offGraphics == null)
				|| (this.getWidth() != offDimension.width)
				|| (this.getHeight()!= offDimension.height) ) 
		{
			offDimension = this.getBounds();
			offImage = createImage(this.getWidth() , this.getHeight());
			offGraphics = offImage.getGraphics();
		}
		createLightGraph();
		//tworzenie swiatla z gradientu
		offGraphics.drawImage(lightImage,0,0,getWidth(),getHeight(),this);
		
		offGraphics.setColor(Color.orange);
		for (int k=0;k<nlights;k++)
			offGraphics.drawRect(lights[k].x-3,lights[k].y-3,6,6);
		// podswietl pojazd
		if (chosenVehicle>=0)
		{
			Vehicle bv=(Vehicle)vehiclelist.get(chosenVehicle);
			offGraphics.setColor(Color.cyan);
			offGraphics.drawOval((int)bv.pos.x-bv.width/2,(int)bv.pos.y-bv.height/2,bv.width,bv.height);
		}
		
		//podswietln swiatlo
		if (chosenLight>=0)
		{
			Point p=lights[chosenLight];
			offGraphics.setColor(Color.pink);
			offGraphics.drawOval((int)p.x-20,(int)p.y-20,40,40);
		}
		if(perimeter)
		{
			offGraphics.setColor(Color.red);
			offGraphics.drawRect(3, 3, getWidth()-7, getHeight()-7);
		}
		for (int i=0;i<vehiclelist.size();i++)
		{
			Vehicle bv=(Vehicle)vehiclelist.get(i);
			bv.paintToArena( offGraphics);
			if(drawTrajectory)
				bv.paintTrajectory(offGraphics);
			if(drawTrail)
				bv.paintTrail(offGraphics);
		}

		g.drawImage(offImage, 0, 0, this);
	}


	public double getSensorValue(double x, double y)
	{
		double val=0;
		for (int k=0;k<nlights;k++)
		{
			val+=MaxIntensity/((lights[k].x-x)*(lights[k].x-x)+(lights[k].y-y)*(lights[k].y-y)+MinDistance*MinDistance);
		}
		if (robotsEmitLight)
		{
			Vehicle bv;
			for (int k=0;k<vehiclelist.size();k++)
			{
				bv=(Vehicle) vehiclelist.get(k);
				val+=MaxIntensity/((bv.pos.x-x)*(bv.pos.x-x)+(bv.pos.y-y)*(bv.pos.y-y)+MinDistance*MinDistance);
			}
		}
		double diff=max-min;
		if (diff<1e-10)
			diff=1;
		return ((val-min)/(diff));
	}

	public void reset()
	{
		lights[0]=new Point();
		lights[0].x=lightDimension.width*2;
		lights[0].y=lightDimension.height*2;
		nlights=1;
		chosenVehicle=-1;
		chosenLight=-1;
		vehiclelist.clear();
		updateLightGraph();		
		repaint();
	}
}

class VehicleGadget extends Canvas
{
	private static final long serialVersionUID = -4970506870547576473L;
	Point []sensors;
	Point []motors;
	int partwidth,partheight;
	Point dragTarget;
	static Color []sensorColors;

	public static boolean isClose(Point p1, Point p2, int tolx, int toly)
	{
		if (Math.abs(p1.x-p2.x)<tolx && Math.abs(p1.y-p2.y)<toly)
			return true;
		return false;
	}
	//funckje pojazdu 
	public VehicleGadget()
	{
		sensors=new Point[2];
		motors=new Point[2];
		dragTarget=null;
		VehicleGadget.sensorColors=new Color[2];
		VehicleGadget.sensorColors[0]=Color.blue;
		VehicleGadget.sensorColors[1]=Color.red;
		updateParts();

		//przeciaganie i upuszczanie
		this.addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e)
			{
				Point p=e.getPoint();
				for (int i=0;i<sensors.length;i++)
				{
					if (VehicleGadget.isClose(p,sensors[i],partwidth,partheight))
					{
						dragTarget=sensors[i];
						return;
					}
				}
			}
			public void mouseReleased(MouseEvent e)
			{
				dragTarget=null;
			}

		});

		this.addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseDragged(MouseEvent e)
			{
				Point p=e.getPoint();
				if (dragTarget!=null)
				{
					if(p.x<0)
						dragTarget.x=0;
					else if(p.x>getWidth())
						dragTarget.x=getWidth();
					else
						dragTarget.x=p.x;
					if(p.y<0)
						dragTarget.y=0;
					else if(p.y>getHeight()-partheight/2)
						dragTarget.y=getHeight()-partheight/2;
					else
						dragTarget.y=p.y;
					repaint();
				}
			}
		});
	}

	// relacja silnikowi czujnikow
	public void updateParts()
	{
		updateParts(false);
	}
	public void updateParts(boolean crossed)
	{
		int i,j;
		if (crossed)
		{
			i=0;
			j=1;
		}
		else
		{
			i=1;
			j=0;
		}
		sensors[i]=new Point(getWidth()/2+partwidth/2+1,partheight/2);
		sensors[j]=new Point(getWidth()/2-partwidth/2-1,partheight/2);
		motors[0]=new Point(getWidth()/2-partwidth/2-1,getHeight()-partheight/2);
		motors[1]=new Point(getWidth()/2+partwidth/2+1,getHeight()-partheight/2);
	}

	//wizualizacja czujnikow
	public void paint(Graphics g)
	{
		g.drawRect(getWidth()/2-5*partwidth/6-1,partheight*3/2, partwidth*7/4, partheight*3);

		for (int i=0;i<sensors.length;i++)
		{
			g.setColor(Color.black);
			g.drawLine(sensors[i].x,sensors[i].y+partheight/2,motors[i].x,motors[i].y);
			g.setColor(sensorColors[i]);
			g.drawOval(sensors[i].x-(partwidth-1)/2,sensors[i].y-(partheight-1)/2,partwidth-1,partheight-1);
			g.drawOval((sensors[i].x-(partwidth-1)/4),(sensors[i].y-(partheight-1)/4),(partwidth-1)/2,(partheight-1)/2);
		}
		g.setColor(Color.black);
		g.fillRect(motors[0].x-partwidth/2+1,motors[0].y-partheight/2,partwidth/2,partheight);
		g.fillRect(motors[1].x,motors[1].y-partheight/2,partwidth/2,partheight);

		g.drawRect(0,0,getBounds().width-1,getBounds().height-1);
	}

	public void setBounds(Rectangle r)
	{
		setBounds(r.x,r.y,r.width,r.height);
	}

	public void setBounds(int x,int y,int w,int h)
	{
		super.setBounds(x,y,w,h);
		partwidth=w/4;
		partheight=h/5;
		updateParts();
	}
	// gives properties of a vehicle
	public VehicleModel getVehicleModel()
	{
		VehicleModel vm=new VehicleModel(this.sensors,this.motors,this.getBounds());
		System.out.println(vm);
		return vm;
	}

}
/** 
 Wektory
 *
 */
class DoubleVector
{
	public double x,y;
	public DoubleVector(Point p)
	{
		x=p.x;
		y=p.y;
	}
	public DoubleVector(double x,double y)
	{
		this.x=x;
		this.y=y;
	}
	public DoubleVector()
	{
	}

	public DoubleVector(DoubleVector v)
	{
		clone(v);
	}

	public void clone(DoubleVector v)
	{
		x=v.x;
		y=v.y;
	}

	public String toString()
	{
		return "["+x+","+y+"]";
	}
	public void equalizePoint(Point p)
	{
		p.x=(int)this.x;
		p.y=(int)this.y;
	}
	public double distance(DoubleVector d)
	{
		double ret;
		ret=(x-d.x)*(x-d.x)+(y-d.y)*(y-d.y);
		return ret;
	}
}
/**
 *model czujnikow
 */
class VehicleModel
{
	DoubleVector [] sensors;
	DoubleVector [] motors;
	public VehicleModel(Point []splist, Point []mplist, Rectangle r)
	{
		sensors=new DoubleVector[splist.length];
		for (int i=0;i<splist.length;i++)
		{
			sensors[i]=new DoubleVector(1.2*(0.5-((double)splist[i].y)/r.height),1.8*(0.5-((double)splist[i].x)/r.width));
		}
		motors=new DoubleVector[mplist.length];
		for( int i=0;i<mplist.length;i++)
		{
			motors[i]=new DoubleVector(1.2*(0.5-((double)mplist[i].y)/r.height),1.8*(0.5-((double)mplist[i].x)/r.width));
		}
	}
}
/**
 * definiowanie poligonow dla rysowania w aplecie
 */
class Hexagon extends Polygon {
	private Point center;  
	private double radius; 
	public Hexagon(int x, int y, double radius) {
		this(new Point(x, y), radius);
	}

	public Hexagon(Point center, double radius) {
		this.center = center;
		this.radius = radius;
		addPoints();
	}
	private void addPoints() {
		int halfHeight = (int)Math.round(radius);
		int halfWidth = (int)Math.round(Math.sqrt(radius*radius*3/4));
		int halfRadius = (int)Math.round(radius / 2.0);

		addPoint(center.x, center.y + halfHeight);
		addPoint(center.x + halfWidth, center.y + halfRadius);
		addPoint(center.x + halfWidth, center.y - halfRadius);
		addPoint(center.x, center.y - halfHeight);
		addPoint(center.x - halfWidth, center.y - halfRadius);
		addPoint(center.x - halfWidth, center.y + halfRadius);
		addPoint(center.x, center.y + halfHeight);
	}
}
class Vehicle
{
	private static final String IMG = null;
	// pozycja elementu x/y 
	DoubleVector pos;
	// kierunekdla vectorow
	double theta;
	// rozmiar
	int width,height;
	// MAX predkosc
	double speed;
	// dlugosc sladu
	int trailLen;
	// limit sladu
	int trailLim;

	VehicleModel model;
	//dodaj tabele
	DoubleVector [] realsensorpos;
	DoubleVector [] realmotorpos;
	DoubleVector [] trajectory;
	DoubleVector [] trail;
	double [] sensorvalues;
	double [] motorspeeds;

	ArenaGadget arena;
	FunctionData [] functions;

	public Vehicle(Point pos,ArenaGadget a, VehicleModel m, FunctionData [] f, double t)
	{
		speed=1;
		this.pos=new DoubleVector(pos);
		arena=a;
		model=m;
		theta=t;
		functions=f.clone();
		width=50;
		height=50;
		trailLen=0;
		trailLim=0;
		trail= new DoubleVector[100000];
		sensorvalues=new double[model.sensors.length];
		motorspeeds=new double[2];
		realsensorpos=new DoubleVector[model.sensors.length];
		realmotorpos=new DoubleVector[model.motors.length];
		for (int i=0;i<realsensorpos.length;i++)
		{
			realsensorpos[i]=new DoubleVector();
			realmotorpos[i]=new DoubleVector();
		}
		updateSensorPositions();
		updateMotorPositions();
		trajectory=null;
	}
	public void updateSensorPositions()
	{
		for (int i=0;i<realsensorpos.length;i++)
		{
			double x,y;
			// calculate relative position
			x=model.sensors[i].x*width;
			y=model.sensors[i].y*height;
			// obrot i wpisz do tabeli 
			realsensorpos[i].x=pos.x+x*Math.cos(theta)+y*Math.sin(theta);
			realsensorpos[i].y=pos.y+x*Math.sin(theta)-y*Math.cos(theta);
		}
	}
	public void updateMotorPositions()
	{
		for (int i=0;i<realmotorpos.length;i++)
		{
			double x,y;
			// pozycja silnikow
			x=model.motors[i].x*width;
			y=model.motors[i].y*height;
			// obrot i transformacja
			realmotorpos[i].x=pos.x+x*Math.cos(theta)+y*Math.sin(theta);
			realmotorpos[i].y=pos.y+x*Math.sin(theta)-y*Math.cos(theta);
		}
	}
	//tylko jeden krok symulacji 
	//w przyszlosci dodac  symulacje krokowa
	// tzn. step > wait > step >...>END
	public void step(int count)
	{
		for (int k=0;k<count;k++)
		{
			for (int i=0;i<model.sensors.length;i++)
			{
				double x,y;
				// obl. poz sensorow
				x=model.sensors[i].x*width;
				y=model.sensors[i].y*height;
				// obrot i transformacja
				realsensorpos[i].x=pos.x+x*Math.cos(theta)+y*Math.sin(theta);
				realsensorpos[i].y=pos.y+x*Math.sin(theta)-y*Math.cos(theta);

				// oblicz pozycje motorow 
				x=model.motors[i].x*width;
				y=model.motors[i].y*height;
				// obrot i transformacja
				realmotorpos[i].x=pos.x+x*Math.cos(theta)+y*Math.sin(theta);
				realmotorpos[i].y=pos.y+x*Math.sin(theta)-y*Math.cos(theta);

				sensorvalues[i]=arena.getSensorValue(realsensorpos[i].x,realsensorpos[i].y);
				motorspeeds[i]=functions[i].getFunctionValue(sensorvalues[i]);
			}
			// aktualizacja polozenia
			double delx,dely,deltheta,delorient;
			delx=((motorspeeds[0]+motorspeeds[1])/2)*Math.cos(theta)*speed;
			dely=((motorspeeds[0]+motorspeeds[1])/2)*Math.sin(theta)*speed;
			// aktualizacja obrotu
			deltheta=0;
			delorient=(motorspeeds[0]-motorspeeds[1])*speed;
			if (Math.abs(delorient)<width)
				deltheta=Math.asin(delorient/width)*10;
			// stop jeeli istnieja warunki 
			if (arena.perimeter)
			{
				if(pos.x+delx>=0 && pos.x+delx<arena.getWidth() && pos.y+dely>=0 && pos.y+dely<arena.getWidth())
				{
					pos.x+=delx;
					pos.y+=dely;
					theta+=deltheta;
				}
			}
			else
			{
				pos.x+=delx;
				pos.y+=dely;
				theta+=deltheta;	
			}
			if (pos.x<0)
			{
				pos.x=arena.getWidth()-1;
			}
			else if (pos.x>=arena.getWidth())
			{
				pos.x=0;
			}
			if (pos.y<0)
			{
				pos.y=arena.getHeight()-1;
			}
			else if (pos.y>=arena.getHeight())
			{
				pos.y=0;
			}
		}
	}
	public void calculateTrail()
	{
		trail[trailLen]=new DoubleVector(pos);
		trailLim++;
		trailLen=trailLim%100000;
	}
	public void calculateTrajectory(int count)
	{
		double thetaold=theta;
		DoubleVector []sensorposold=new DoubleVector[realsensorpos.length];
		DoubleVector []motorposold=new DoubleVector[realmotorpos.length];
		for (int i=0;i<realsensorpos.length;i++)
			sensorposold[i]=new DoubleVector(realsensorpos[i]);
		for (int i=0;i<realmotorpos.length;i++)
			motorposold[i]=new DoubleVector(realmotorpos[i]);
		trajectory=new DoubleVector[count];
		for (int i=0;i<count;i++)
		{
			trajectory[i]=new DoubleVector(pos);
			step(1);
		}
		pos.clone(trajectory[0]);
		theta=thetaold;		
		for (int i=0;i<realsensorpos.length;i++)
			realsensorpos[i].clone(sensorposold[i]);
		for (int i=0;i<realmotorpos.length;i++)
			realmotorpos[i].clone(motorposold[i]);
	}
// drawing vehicle to arena
	public void paintToArena( Graphics g)
	{
		// for sensors
		for (int i=0;i<model.sensors.length;i++)
		{
			g.setColor(VehicleGadget.sensorColors[i]);
			g.fillOval((int)(realsensorpos[i].x-width/5),(int)(realsensorpos[i].y-height/5),2*width/5,2*height/5);
			g.drawLine((int)(realmotorpos[i].x),(int)(realmotorpos[i].y),(int)realsensorpos[i].x,(int)realsensorpos[i].y);
			g.setColor(Color.white);
			g.drawOval((int)(realsensorpos[i].x-width/10),(int)(realsensorpos[i].y-height/10),width/5,height/5);
			g.drawOval((int)(realsensorpos[i].x-width/5),(int)(realsensorpos[i].y-height/5),2*width/5,2*height/5);
			
		}
		
		int i,j;
		if((realsensorpos[0].distance(realmotorpos[0])<=realsensorpos[0].distance(realmotorpos[1])) &&
				(realsensorpos[1].distance(realmotorpos[0])>=realsensorpos[1].distance(realmotorpos[1])))
		{
			i=0;
			j=1;
		}
		else
		{
			i=1;
			j=0;
		}
		int [] xPoints = {(int)(realsensorpos[i].x),(int)(realsensorpos[j].x),(int)(realmotorpos[1].x),(int)(realmotorpos[0].x)};
		int [] yPoints = {(int)(realsensorpos[i].y),(int)(realsensorpos[j].y),(int)(realmotorpos[1].y),(int)(realmotorpos[0].y)};
		g.fillPolygon(xPoints, yPoints, 4);
			
		//for motors
		g.setColor(Color.darkGray);
		int [] x0Points = {(int)(realmotorpos[0].x+(realmotorpos[0].x-pos.x)/2),
				(int)((realmotorpos[0].x-realmotorpos[1].x)/2+realmotorpos[0].x-(realmotorpos[0].x-pos.x)/2),
				(int)(realmotorpos[0].x-(realmotorpos[0].x-pos.x)/2),
				(int)(realmotorpos[0].x+(realmotorpos[0].x-pos.x)/2-(realmotorpos[0].x-realmotorpos[1].x)/2)};

		int [] y0Points = {(int)(realmotorpos[0].y+(realmotorpos[0].y-pos.y)/2),
				(int)((realmotorpos[0].y-realmotorpos[1].y)/2+realmotorpos[0].y-(realmotorpos[0].y-pos.y)/2),
				(int)(realmotorpos[0].y-(realmotorpos[0].y-pos.y)/2),
				(int)(realmotorpos[0].y+(realmotorpos[0].y-pos.y)/2-(realmotorpos[0].y-realmotorpos[1].y)/2)};

		g.fillPolygon(x0Points,y0Points,4);

		int [] x1Points = {(int)(realmotorpos[1].x-(pos.x-realmotorpos[1].x)/2),
				(int)(realmotorpos[1].x+(pos.x-realmotorpos[1].x)/2-(realmotorpos[0].x-realmotorpos[1].x)/2),
				(int)(realmotorpos[1].x+(pos.x-realmotorpos[1].x)/2),
				(int)(realmotorpos[1].x-(pos.x-realmotorpos[1].x)/2+(realmotorpos[0].x-realmotorpos[1].x)/2)};

		int [] y1Points = {(int)(realmotorpos[1].y+(realmotorpos[1].y-pos.y)/2),
				(int)(realmotorpos[1].y-(realmotorpos[1].y-pos.y)/2-(realmotorpos[0].y-realmotorpos[1].y)/2),
				(int)(realmotorpos[1].y-(realmotorpos[1].y-pos.y)/2),
				(int)(realmotorpos[1].y+(realmotorpos[1].y-pos.y)/2+(realmotorpos[0].y-realmotorpos[1].y)/2)};

		g.fillPolygon(x1Points,y1Points,4);
		g.setColor(Color.white);
		g.drawPolygon(x0Points,y0Points,4);
		g.drawPolygon(x1Points,y1Points,4);
		
	}

	public void paintTrajectory(Graphics g)
	{
		if (trajectory!=null )
		{
			g.setColor(Color.green);
			for (int i=1;i<trajectory.length;i++)
			{
				if((Math.abs(trajectory[i-1].x-trajectory[i].x)<10) && (Math.abs(trajectory[i-1].y-trajectory[i].y)<10)) 
					g.drawLine((int)trajectory[i-1].x,(int)trajectory[i-1].y,(int)trajectory[i].x,(int)trajectory[i].y);
			}
		}
	}
	public void paintTrail(Graphics g) 
	{
		if(trail!=null)
		{
			 
			g.setColor(Color.white);
			for(int i=1,j=1;j<trailLim;j=j+2,i=j%100000)
			{
				i=(i==0?1:i);
				if( i%20<10 && (Math.abs(trail[i-1].x-trail[i].x)<10) && (Math.abs(trail[i-1].y-trail[i].y)<10))
				{
					g.setXORMode(Color.black);
					g.drawLine((int)trail[i-1].x,(int)trail[i-1].y,(int)trail[i].x,(int)trail[i].y);
					g.setPaintMode();
				}
			}
		/*		try {
				//	BufferedImage image =ImageIO.read(new File("img/Snuke.png"));
					
					//obrazek rakiety, w przyszlosci dodac obracajaca sie grafike
					//linux
			// String workingDir = System.getProperty("user.dir");
			// System.out.println(workingDir);
			// BufferedImage image2 = ImageIO.read(new File(workingDir+"/Snuke.png"));
	    	// g.drawImage(image, (int) pos.x-20,(int) pos.y-30, null);
	    	
	    	 
	    	 } catch (IOException ex) {
		           System.out.println(ex);
		       }*/
		}
	}
}
