import java.applet.Applet;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Random;

public class Braitenberg extends Applet implements Runnable{
	FunctionGraphGadget funcL;
	FunctionGraphGadget funcR;
	ArenaGadget arena;
	VehicleGadget vehicleConstructor;
	private volatile Thread animationThread = null;
	volatile boolean paused;
	Button btnRun;
	Button btnStep;
	Button btnReset;
	Button btnCreate;
	Button btnCreateL;
	Button btnDelR;
	Button btnDelL;
	Checkbox cbDrawTrail;
	Choice chVehicle;
	int simSpeed=1;
	Random rnd=new Random();

	public void init() {
		this.setLayout(null);
	// inicjalizacja areny
		arena=new ArenaGadget();
		//DEF: 0/20/650/650  //1050
		arena.setBounds(1,50,1150,650);
		this.add(arena);
		//inicjacja funkcji
		funcL=new FunctionGraphGadget();
		funcL.setBounds(505,20,190,190);
		funcR=new FunctionGraphGadget();
		funcR.setBounds(505,1360,190,190);
		
		// sensor modification box
		vehicleConstructor=new VehicleGadget();
		//670 >> 770
		//schemat maszyn 
		vehicleConstructor.setBounds(100,100,190,190);
		this.add(vehicleConstructor);
		// wybór rodzaju pojazdu
		chVehicle=new Choice();
		//670/230/190/30
		String opt2a = "2.a - ucieka";
		String opt2b = "2.b - dazy";
		String opt3a = "3.a - \"love\"";
		String opt3b = "3.b - poszukuje kolejnych" ;
		String opt4a = "4.a - satelita";
		chVehicle.setBounds(70,10,190,30);
		chVehicle.add(opt2a);
		chVehicle.add(opt2b);
		chVehicle.add(opt3a);//
		chVehicle.add(opt3b);//
		chVehicle.add(opt4a);//
		chVehicle.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e)
			{
				String str=chVehicle.getSelectedItem();
				if (str.compareTo(opt2a)==0)
				{
					vehicleConstructor.updateParts(false);
					vehicleConstructor.repaint();
					funcL.fillLinear();
					funcR.fillLinear();
				}
				else if (str.compareTo(opt2b)==0)
				{
					vehicleConstructor.updateParts(true);
					vehicleConstructor.repaint();
					funcL.fillLinear();
					funcR.fillLinear();
				}
				else if (str.compareTo(opt3a)==0)
				{
					vehicleConstructor.updateParts(false);
					vehicleConstructor.repaint();
					funcL.fillInverse();
					funcR.fillInverse();
				}
				else if (str.compareTo(opt3b)==0)
				{
					vehicleConstructor.updateParts(true);
					vehicleConstructor.repaint();
					funcL.fillInverse();
					funcR.fillInverse();
				}
				else if(str.compareTo(opt4a)==0)
				{
					vehicleConstructor.updateParts(false);
					vehicleConstructor.repaint();
					funcL.fillHill();
					funcR.fillHill();
				}
				
							}
		});
		add(chVehicle);

		btnCreate=new Button();
		btnCreate.setBounds(300,1,100,50);
		btnCreate.setLabel("Create Rocket");
		btnCreate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				VehicleModel m=vehicleConstructor.getVehicleModel();
				FunctionData [] f=new FunctionData[2];
				f[0]=(FunctionData)funcL.dat.clone();
				f[1]=(FunctionData)funcR.dat.clone();
				int tmp1=(rnd.nextInt(1050));
				int tmp2=(rnd.nextInt(650));
				int tmp3=(rnd.nextInt(360));
				Vehicle vehicle=new Vehicle(new Point(tmp1,tmp2),arena,m,f,tmp3);
				arena.vehiclelist.add(vehicle);
				setCursor(new Cursor(Cursor.WAIT_CURSOR));
				if(arena.drawTrajectory) vehicle.calculateTrajectory(arena.trajectoryLength);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				arena.repaint();
			}
		});
		this.add(btnCreate);

		btnDelR=new Button();
		btnDelR.setBounds(400,1,100,50);
		btnDelR.setLabel("Delete Rocket");
		btnDelR.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if(arena.chosenVehicle!=-1)
				{
					setCursor(new Cursor(Cursor.WAIT_CURSOR));
					arena.vehiclelist.remove(arena.chosenVehicle);
					arena.chosenVehicle=-1;
					if(arena.robotsEmitLight) arena.updateLightGraph();
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					
					arena.repaint();
				}
			}
		});
		this.add(btnDelR);

		btnCreateL=new Button();
		btnCreateL.setBounds(500,1,100,50);
		btnCreateL.setLabel("Create Light");
		btnCreateL.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if (arena.nlights<arena.lights.length-1)
				{
					int tmp1=(rnd.nextInt(1000));
					int tmp2=(rnd.nextInt(650));
					arena.lights[arena.nlights++]=new Point(tmp1,tmp2);
					arena.updateLightGraph();
					for(int i=0;arena.drawTrajectory && i<arena.vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) arena.vehiclelist.get(i);
						bv.calculateTrajectory(arena.trajectoryLength);
						repaint();
					}
				}
			}
		});
		this.add(btnCreateL);

		btnDelL=new Button();
		btnDelL.setBounds(600,1,100,50);
		btnDelL.setLabel("Delete Light");
		btnDelL.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if(arena.chosenLight!=-1)
					for (int i=0;i<arena.nlights;i++)
					{
						if (arena.chosenLight==i && arena.nlights>1)
						{
							arena.chosenLight=-1;
							arena.lights[i]=arena.lights[arena.nlights-1];
							arena.nlights--;
							arena.updateLightGraph();
							for(int j=0;arena.drawTrajectory && j<arena.vehiclelist.size();j++)
							{
								Vehicle bv=(Vehicle) arena.vehiclelist.get(j);
								bv.calculateTrajectory(arena.trajectoryLength);
								repaint();
							}
							return;
						}
					}
			}
		});
		this.add(btnDelL);

		// aktywator sladu
		cbDrawTrail=new Checkbox();
	//	cbDrawTrail.setBounds(670,370,190,20);
	//	cbDrawTrail.setLabel("Draw Trails");
	//	cbDrawTrail.setState(false);
		cbDrawTrail.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e)
			{
				arena.drawTrail=cbDrawTrail.getState();
			
				for(int i=0;i<arena.vehiclelist.size();i++)
				{
					Vehicle bv=(Vehicle) arena.vehiclelist.get(i);
					bv.trail=new DoubleVector[100000];
					bv.trailLen=0;
					bv.trailLim=0;
				}
				repaint();
			}
		});
//	add(cbDrawTrail);
// arena color 
		
	//	arena.colorOp=7;
		paused=true;

		btnRun=new Button();
		btnRun.setBounds(700,1,100,50);
		btnRun.setLabel("Run");
		btnRun.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				if (paused)
				{
					arena.drawTrail=true;
					for(int i=0;i<arena.vehiclelist.size();i++)
					{
						Vehicle bv=(Vehicle) arena.vehiclelist.get(i);
						bv.trailLen=0;
						bv.trailLim=0;
					}
					Component [] cm=getComponents();
					for (int i=0;i<cm.length;i++)
						cm[i].setEnabled(false);
				
					cbDrawTrail.setEnabled(true);	
					btnRun.setLabel("Pause");
					btnRun.setEnabled(true);
					paused=false;

				}
				else 
				{	
					Component [] cm=getComponents();
					for (int i=0;i<cm.length;i++)
						cm[i].setEnabled(true);
						btnRun.setLabel("Run");
					paused=true;
				}
			}
		});
		add(btnRun);

		btnStep=new Button();
		btnStep.setBounds(800,1,100,50);
		btnStep.setLabel("Step");
		btnStep.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				for (int i=0;i<arena.vehiclelist.size();i++)
				{
					Vehicle bv=(Vehicle)arena.vehiclelist.get(i);
					bv.step(10);
				}
				if(arena.robotsEmitLight) arena.updateLightGraph();
				arena.repaint();
			}
		});
		this.add(btnStep);		

		btnReset=new Button();
		btnReset.setBounds(900,1,100,50);
		btnReset.setLabel("Reset");
		btnReset.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				arena.reset();
			}
		});
		add(btnReset);

	}

	public void paint(Graphics g) {}

	public void start() 
	{
		if (animationThread == null) 
		{
			paused=true;
			animationThread = new Thread(this, "Animation");
			animationThread.start();
		}
	}

	public void run() 
	{
		int stepcount=0;
		int LightUpdatePeriod=10;
		long lastTimer=0;
		Thread myThread = Thread.currentThread();
		while (animationThread == myThread) 
		{
			
			if (!paused)
			{
				lastTimer=System.currentTimeMillis();
				for (int i=0;i<arena.vehiclelist.size();i++)
				{
					Vehicle bv=(Vehicle)arena.vehiclelist.get(i);
					bv.step(1);
					if(arena.drawTrail)
						bv.calculateTrail();
				}
				
				arena.repaint();
			}
			try 
			{
				
				long t=(22-simSpeed)-(System.currentTimeMillis()-lastTimer);
				if (t>0 && !paused)
					Thread.sleep(t);
				else
					Thread.sleep(10);
			} catch (InterruptedException e){ }
			stepcount++;
		}
	}	
}