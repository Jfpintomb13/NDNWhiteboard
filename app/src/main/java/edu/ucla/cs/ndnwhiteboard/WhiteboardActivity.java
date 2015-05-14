package edu.ucla.cs.ndnwhiteboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;


public class WhiteboardActivity extends ActionBarActivity {

    private DrawingView drawingView_canvas;
    private ImageButton button_pencil;
    private ImageButton button_eraser;
    private ImageButton button_color;
    private ImageButton button_save;
    private ImageButton button_undo;
    private ImageButton button_clear;
    private String username;
    private String whiteboard;
    private String prefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whiteboard);

        Intent introIntent = getIntent();
        this.username = introIntent.getExtras().getString("name");
        this.whiteboard = introIntent.getExtras().getString("whiteboard").replaceAll("\\s","");
        this.prefix = introIntent.getExtras().getString("prefix");
        Log.i("WhiteboardActivity", "username: " + this.username);
        Log.i("WhiteboardActivity", "whiteboard: " + this.whiteboard);
        Log.i("WhiteboardActivity", "prefix: " + this.prefix);
        Toast.makeText(getApplicationContext(), "Welcome " + this.username, Toast.LENGTH_SHORT).show();

        drawingView_canvas = (DrawingView) findViewById(R.id.drawingview_canvas);
        button_pencil = (ImageButton) findViewById(R.id.button_pencil);
        button_eraser = (ImageButton) findViewById(R.id.button_eraser);
        button_color = (ImageButton) findViewById(R.id.button_color);
        button_save = (ImageButton) findViewById(R.id.button_save);
        button_undo = (ImageButton) findViewById(R.id.button_undo);
        button_clear = (ImageButton) findViewById(R.id.button_clear);
        drawingView_canvas.setActivity(this);

        button_pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView_canvas.setPencil();
            }
        });

        button_eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView_canvas.setEraser();
            }
        });

        button_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView_canvas.incrementColor();
            }
        });

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSave();
            }
        });

        button_undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView_canvas.undo();
            }
        });

        button_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmErase();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        new FetchTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_whiteboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void callback(String string) {
        //TODO: implement callback
        Log.i("WhiteboardActivity", "callback" + string);
    }

    public void setButtonColor(int color) {
        button_color.setBackgroundColor(color);
    }

    private void confirmErase() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Confirm erase")
            .setMessage("Are you sure you want to erase the canvas?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    drawingView_canvas.clear();
                }

            })
            .setNegativeButton("No", null)
            .show();
    }

    private void confirmSave() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Confirm canvas save")
            .setMessage("Do you want to save the canvas?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    drawingView_canvas.setDrawingCacheEnabled(true);
                    Date date = new Date();
                    Format formatter = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
                    String fileName = formatter.format(date) + ".png";
                    if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                        File sdCard = Environment.getExternalStorageDirectory();
                        File dir = new File(sdCard.getAbsolutePath() + "/NDN_Whiteboard");
                        dir.mkdirs();
                        File file = new File(dir, fileName);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        drawingView_canvas.getDrawingCache().compress(Bitmap.CompressFormat.PNG, 100, baos);
                        FileOutputStream f = null;
                        try {
                            f = new FileOutputStream(file);
                            if (f != null) {
                                f.write(baos.toByteArray());
                                f.flush();
                                f.close();
                                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Save Failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    drawingView_canvas.destroyDrawingCache();
                }
            })
            .setNegativeButton("No", null)
            .show();
    }

    public void drawInitialCanvas() {
        String initialWhiteboard =
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.08914728462696075,0.30135658383369446],[0.08914728462696075,0.30135658383369446],[0.08769379556179047,0.30135658383369446],[0.08643974363803864,0.30038759112358093],[0.08624030649662018,0.2991163730621338],[0.08478682488203049,0.2994185984134674],[0.09295739233493805,0.321803480386734],[0.10584263503551483,0.3535800278186798],[0.12167467921972275,0.38410547375679016],[0.1364704966545105,0.4080814719200134],[0.14571797847747803,0.4179048240184784],[0.15451866388320923,0.42543870210647583],[0.16144132614135742,0.42360177636146545],[0.1659938097000122,0.4164960980415344],[0.17141667008399963,0.3968401253223419],[0.17292916774749756,0.3835598826408386],[0.17248061299324036,0.37991735339164734],[0.17248061299324036,0.37841618061065674],[0.17633605003356934,0.38176241517066956],[0.1876412332057953,0.3896290361881256],[0.2017991542816162,0.39641737937927246],[0.21392174065113068,0.39371034502983093],[0.22519978880882263,0.3772181570529938],[0.23517002165317535,0.3405390977859497],[0.23747749626636505,0.30851486325263977],[0.23586204648017883,0.28559574484825134],[0.23546510934829712,0.26827678084373474],[0.23546510934829712,0.2549489438533783],[0.23546510934829712,0.25193798542022705]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.25872093439102173,0.3711240291595459],[0.25872093439102173,0.3711240291595459],[0.25872093439102173,0.3711240291595459],[0.25872093439102173,0.3704007863998413],[0.26235365867614746,0.3677332103252411],[0.27242380380630493,0.3549644351005554],[0.28414344787597656,0.33659541606903076],[0.29183363914489746,0.3208034932613373],[0.29517173767089844,0.3111901581287384],[0.29378724098205566,0.30832210183143616],[0.2897219955921173,0.30813953280448914],[0.2773516774177551,0.3203679025173187],[0.2660648822784424,0.3368523120880127],[0.2610100209712982,0.35113319754600525],[0.2606589198112488,0.36047008633613586],[0.2759090065956116,0.366465300321579],[0.30418840050697327,0.36855244636535645],[0.3326708674430847,0.3585161864757538],[0.3573595881462097,0.3484629690647125],[0.3633720874786377,0.3420542776584625]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.3265503942966461,0.2422480583190918],[0.3265503942966461,0.2422480583190918],[0.32415398955345154,0.2422480583190918],[0.32461240887641907,0.2460995465517044],[0.3260580599308014,0.2605804204940796],[0.333416223526001,0.2880890667438507],[0.34219327569007874,0.3138628304004669],[0.3510049283504486,0.3358073830604553],[0.36131924390792847,0.35367295145988464],[0.3643410801887512,0.35755813121795654]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.4156976640224457,0.26259690523147583],[0.4156976640224457,0.26259690523147583],[0.4156976640224457,0.26259690523147583],[0.4156976640224457,0.26259690523147583],[0.4156976640224457,0.26259690523147583],[0.41361501812934875,0.26259690523147583],[0.4098779261112213,0.26712656021118164],[0.4055006802082062,0.2741008996963501],[0.40014997124671936,0.28369393944740295],[0.39690908789634705,0.29279983043670654],[0.39866065979003906,0.30325815081596375],[0.4027681052684784,0.31468135118484497],[0.410177081823349,0.3232418894767761],[0.421458899974823,0.330635666847229],[0.44250091910362244,0.32844382524490356],[0.4614570438861847,0.3166359066963196],[0.4689922332763672,0.3071705400943756]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.4893410801887512,0.24127906560897827],[0.4893410801887512,0.24127906560897827],[0.4880123734474182,0.24127906560897827],[0.4865487217903137,0.24127906560897827],[0.48508119583129883,0.24127906560897827],[0.48000821471214294,0.24488724768161774],[0.47415900230407715,0.2520202398300171],[0.4691922962665558,0.2593565285205841],[0.46619951725006104,0.2666717767715454],[0.46797940135002136,0.27308040857315063],[0.46944740414619446,0.2775282859802246],[0.47567808628082275,0.28094950318336487],[0.48441776633262634,0.283888578414917],[0.49418070912361145,0.2810095250606537],[0.5019840002059937,0.27514779567718506],[0.505864143371582,0.2653030753135681],[0.5042455792427063,0.2526004910469055],[0.4978100061416626,0.24189527332782745],[0.4863242208957672,0.23313087224960327],[0.4742404520511627,0.2275010347366333],[0.46404919028282166,0.22520747780799866],[0.46124032139778137,0.22480620443820953]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.5377907156944275,0.23740309476852417],[0.5377907156944275,0.23740309476852417],[0.536607563495636,0.23740309476852417],[0.5351405143737793,0.23740309476852417],[0.5336790084838867,0.23981262743473053],[0.5322057008743286,0.24520818889141083],[0.5319767594337463,0.2510736286640167],[0.5327408909797668,0.25616827607154846],[0.5342053174972534,0.2593020796775818],[0.53646320104599,0.25968992710113525],[0.5419537425041199,0.25584086775779724],[0.5503810048103333,0.24290502071380615],[0.5578635931015015,0.22826515138149261],[0.5617419481277466,0.22147735953330994],[0.5646662712097168,0.21695414185523987],[0.5675969123840332,0.21705426275730133],[0.571873128414154,0.2197500467300415],[0.5771419405937195,0.2299596518278122],[0.581636905670166,0.24022769927978516],[0.5851343274116516,0.2449495941400528],[0.5866695046424866,0.2465532273054123],[0.5900235176086426,0.24335607886314392],[0.5943630933761597,0.23472340404987335],[0.598800778388977,0.22010691463947296],[0.6017209887504578,0.21275176107883453],[0.6036867499351501,0.2102622091770172],[0.6071249842643738,0.20930232107639313],[0.6125249862670898,0.2113012820482254],[0.6183830499649048,0.21625128388404846],[0.6242400407791138,0.22210822999477386],[0.6306573748588562,0.22645646333694458],[0.6390274167060852,0.2308509349822998],[0.651493489742279,0.23255814611911774],[0.6635761260986328,0.23042380809783936],[0.6756171584129333,0.22322514653205872],[0.6848528981208801,0.2121858298778534],[0.690199077129364,0.19151584804058075],[0.6886834502220154,0.17376738786697388],[0.6834686994552612,0.1644134521484375],[0.6742194890975952,0.15938307344913483],[0.6607694625854492,0.16315992176532745],[0.6485539674758911,0.1741204708814621],[0.6373059749603271,0.1948002725839615],[0.6344671845436096,0.21056081354618073],[0.6409527063369751,0.22354143857955933],[0.6628410220146179,0.23777469992637634],[0.6962539553642273,0.24127906560897827],[0.7266172170639038,0.2375190258026123]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.18023255467414856,0.5455426573753357],[0.18023255467414856,0.5455426573753357],[0.18023255467414856,0.5434620976448059],[0.18023255467414856,0.542281448841095],[0.18354621529579163,0.5403411984443665],[0.20586366951465607,0.5327999591827393],[0.2491883635520935,0.5143698453903198],[0.2883388102054596,0.4965418875217438],[0.30038759112358093,0.4893410801887512]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.2315891534090042,0.4593023359775543],[0.2315891534090042,0.4593023359775543],[0.23044700920581818,0.4593023359775543],[0.2292991429567337,0.4593023359775543],[0.22965116798877716,0.46384724974632263],[0.22965116798877716,0.47097447514533997],[0.23054587841033936,0.48382768034935],[0.23340165615081787,0.4997943639755249],[0.2391776740550995,0.5160643458366394],[0.25064390897750854,0.5277757048606873],[0.27770859003067017,0.5329226851463318],[0.2982458770275116,0.5243120789527893],[0.3071803152561188,0.5096759796142578],[0.31203746795654297,0.4989651143550873],[0.31298449635505676,0.4931366443634033],[0.31149768829345703,0.49173030257225037],[0.3100287616252899,0.49127909541130066],[0.30705752968788147,0.49538108706474304],[0.3062015473842621,0.5012581944465637],[0.30887702107429504,0.5065513849258423],[0.3185991048812866,0.5098823308944702],[0.33283427357673645,0.5073277950286865],[0.3427671492099762,0.5014626979827881],[0.3465448021888733,0.49560365080833435],[0.3457810580730438,0.48859503865242004],[0.3391660153865814,0.48289358615875244],[0.32809978723526,0.47703438997268677],[0.31771382689476013,0.4734756648540497],[0.31123921275138855,0.4717051088809967],[0.309666246175766,0.4681696891784668],[0.30910852551460266,0.4670542776584625]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.44864341616630554,0.3798449635505676],[0.44864341616630554,0.3798449635505676],[0.44636473059654236,0.38246431946754456],[0.442256361246109,0.40209025144577026],[0.4365273714065552,0.42806869745254517],[0.43335291743278503,0.45361268520355225],[0.4304211735725403,0.4730958044528961],[0.43023255467414856,0.48207584023475647],[0.43023255467414856,0.48808926343917847],[0.43023255467414856,0.4846852421760559],[0.4343668818473816,0.4693368673324585]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.4389534890651703,0.38372093439102173],[0.4389534890651703,0.38372093439102173],[0.4389534890651703,0.38480570912361145],[0.44758740067481995,0.39621835947036743],[0.4660142958164215,0.41369760036468506],[0.4924837052822113,0.43428218364715576],[0.5198643207550049,0.44864341616630554],[0.5411691665649414,0.45655250549316406],[0.5513566136360168,0.45639535784721375]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.5174418687820435,0.3711240291595459],[0.5174418687820435,0.3711240291595459],[0.5174418687820435,0.3711240291595459],[0.5174418687820435,0.3711240291595459],[0.5174418687820435,0.372162401676178],[0.5196829438209534,0.3788163661956787],[0.5248735547065735,0.39099451899528503],[0.5328543782234192,0.4093627333641052],[0.5406915545463562,0.43002164363861084],[0.5463050007820129,0.4463794231414795],[0.5510076880455017,0.4601936638355255],[0.5544283986091614,0.46668297052383423],[0.5576640367507935,0.47045472264289856]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.588178277015686,0.3343023359775543],[0.588178277015686,0.3343023359775543],[0.5872092843055725,0.3343023359775543],[0.5872092843055725,0.33562836050987244],[0.5889162421226501,0.3485354781150818],[0.5945308208465576,0.3726862370967865],[0.6022571921348572,0.3968878388404846],[0.6095786690711975,0.41473299264907837],[0.6150397658348083,0.4249144494533539],[0.6194942593574524,0.43096646666526794],[0.6191860437393188,0.4312015473842621],[0.6154032349586487,0.4241359531879425],[0.6124030947685242,0.41763564944267273]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.5939922332763672,0.3449612259864807],[0.5939922332763672,0.3449612259864807],[0.5939922332763672,0.3449612259864807],[0.5939922332763672,0.3449612259864807],[0.6000053882598877,0.345970094203949],[0.6209232807159424,0.35300126671791077],[0.6433692574501038,0.36283591389656067],[0.6605448126792908,0.37455984950065613],[0.6720314025878906,0.38520967960357666],[0.6789988875389099,0.3995462954044342],[0.678099513053894,0.4136684238910675],[0.668239176273346,0.4261431396007538],[0.647360622882843,0.4390330910682678],[0.6199284195899963,0.44621744751930237],[0.5978809595108032,0.44878092408180237],[0.5856878161430359,0.44801077246665955],[0.5788877606391907,0.4432288408279419]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.7073643207550049,0.3032945692539215],[0.7073643207550049,0.3032945692539215],[0.7073643207550049,0.3085770010948181],[0.7073643207550049,0.3240620195865631],[0.7073643207550049,0.35012757778167725],[0.7059949040412903,0.3750252425670624],[0.7063953280448914,0.3927634656429291],[0.7063953280448914,0.4039953649044037],[0.7063953280448914,0.40878644585609436],[0.7058979272842407,0.40742239356040955],[0.7054263353347778,0.4060077667236328]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.6976743936538696,0.32364341616630554],[0.6976743936538696,0.32364341616630554],[0.6986675262451172,0.3256295919418335],[0.7092722058296204,0.3356318473815918],[0.7294979095458984,0.3522029519081116],[0.7529621124267578,0.36752480268478394],[0.7730206847190857,0.3788238763809204],[0.7866937518119812,0.3840155303478241],[0.7955138087272644,0.38353291153907776]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.7625969052314758,0.2848837077617645],[0.7625969052314758,0.2848837077617645],[0.7615072131156921,0.2848837077617645],[0.7616279125213623,0.2907644510269165],[0.7658618092536926,0.31069454550743103],[0.7714543342590332,0.33636996150016785],[0.7782744765281677,0.3601909875869751],[0.7841372489929199,0.37984129786491394],[0.7891936898231506,0.3919842839241028],[0.7923321723937988,0.40252313017845154]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.058139536529779434,0.645348846912384],[0.058139536529779434,0.645348846912384],[0.058812808245420456,0.649010956287384],[0.06261473149061203,0.6607124209403992],[0.07671823352575302,0.6916483640670776],[0.09019417315721512,0.7215339541435242],[0.10193590819835663,0.7428810000419617],[0.11219314485788345,0.7570667266845703],[0.1189887598156929,0.7626328468322754],[0.12628936767578125,0.7647927403450012],[0.1356566995382309,0.7551709413528442],[0.14441733062267303,0.7357006072998047],[0.14963456988334656,0.7256450653076172],[0.15811942517757416,0.7195224165916443],[0.17580968141555786,0.7251921892166138],[0.19221168756484985,0.7352482080459595],[0.20470790565013885,0.7455852627754211],[0.21221601963043213,0.7515558004379272],[0.21407084167003632,0.751937985420227],[0.21554289758205414,0.7463555335998535],[0.20856836438179016,0.7019446492195129],[0.19841411709785461,0.6594874858856201],[0.19098049402236938,0.6292498707771301],[0.1851331740617752,0.6058427691459656],[0.18025894463062286,0.596000611782074],[0.1758720874786377,0.5872092843055725]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.21511627733707428,0.5813953280448914],[0.21511627733707428,0.5813953280448914],[0.21511627733707428,0.5813953280448914],[0.21618181467056274,0.5845919847488403],[0.22043249011039734,0.6010888814926147],[0.2262781709432602,0.627389132976532],[0.23153413832187653,0.6524844169616699],[0.23592878878116608,0.6725518107414246],[0.23870684206485748,0.6838266253471375],[0.2404833883047104,0.6894734501838684],[0.24176356196403503,0.6918604373931885],[0.24234434962272644,0.6898261904716492],[0.24695435166358948,0.6828631162643433],[0.2528166174888611,0.6716923713684082],[0.25938960909843445,0.6623758673667908],[0.26908084750175476,0.6548370718955994],[0.28350740671157837,0.6540697813034058],[0.2993444800376892,0.6565227508544922],[0.31278616189956665,0.6636605858802795],[0.32348403334617615,0.669620931148529],[0.32751938700675964,0.6724806427955627]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.3062015473842621,0.6027131676673889],[0.3062015473842621,0.6027131676673889],[0.3043709099292755,0.6027131676673889],[0.30426356196403503,0.6027131676673889],[0.30426356196403503,0.601259708404541],[0.30543723702430725,0.6005704402923584],[0.31916967034339905,0.5943244099617004],[0.3268383741378784,0.5883355140686035]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.33236435055732727,0.6279069781303406],[0.33236435055732727,0.6279069781303406],[0.33236435055732727,0.6318408846855164],[0.3343678414821625,0.6432212591171265],[0.3373038172721863,0.6554167866706848],[0.34023404121398926,0.6645386219024658],[0.3421255946159363,0.6697876453399658],[0.34302327036857605,0.6720727682113647],[0.34302327036857605,0.6724806427955627],[0.34302327036857605,0.6703214645385742],[0.34410977363586426,0.6663140654563904],[0.3455773890018463,0.6619112491607666],[0.34815648198127747,0.6575133800506592],[0.3568664789199829,0.6508683562278748],[0.371938019990921,0.6424382925033569],[0.3936425745487213,0.6329667568206787],[0.4168149530887604,0.6226942539215088],[0.43358248472213745,0.6123815774917603],[0.4360465109348297,0.6104651093482971]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.38468992710113525,0.5901162624359131],[0.38468992710113525,0.5901162624359131],[0.3834097385406494,0.5901162624359131],[0.38372093439102173,0.5949890613555908],[0.38455089926719666,0.6072759032249451],[0.386016845703125,0.6217963695526123],[0.3883342146873474,0.6364384293556213],[0.39128559827804565,0.6498356461524963],[0.3942068815231323,0.6575133800506592],[0.397143691778183,0.6634734272956848],[0.401851624250412,0.6655576229095459],[0.40909087657928467,0.6684812903404236],[0.42209455370903015,0.6667096018791199],[0.43520447611808777,0.6638391613960266],[0.4522995352745056,0.6522917151451111],[0.4669557809829712,0.6376954317092896],[0.47774162888526917,0.6152490377426147],[0.48345503211021423,0.5952136516571045],[0.48056289553642273,0.5841019749641418],[0.4762811064720154,0.5799632668495178],[0.4631284773349762,0.5823808908462524],[0.4494520425796509,0.5946531295776367],[0.43777701258659363,0.6117897629737854],[0.43358656764030457,0.6258217096328735],[0.4351620674133301,0.6369238495826721],[0.4443977177143097,0.6437704563140869],[0.46628493070602417,0.6494311094284058],[0.47868216037750244,0.6472868323326111]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.5048449635505676,0.5067829489707947],[0.5048449635505676,0.5067829489707947],[0.5048449635505676,0.5067829489707947],[0.5062230825424194,0.5128553509712219],[0.5115151405334473,0.5310975313186646],[0.5214353203773499,0.5604299902915955],[0.531669557094574,0.5875269174575806],[0.5394836068153381,0.6058561205863953],[0.5443710088729858,0.617098331451416],[0.546291172504425,0.6206830739974976],[0.5469961166381836,0.6220930218696594],[0.5424718856811523,0.6115092039108276],[0.5353513360023499,0.5880240797996521],[0.5313686728477478,0.574298620223999],[0.532287061214447,0.56499844789505],[0.5421483516693115,0.5605629682540894],[0.557866632938385,0.5600775480270386],[0.5741779208183289,0.5679563879966736],[0.5861286520957947,0.5780273079872131],[0.5925453305244446,0.5936470031738281],[0.5930232405662537,0.6097192168235779],[0.5830527544021606,0.6241148710250854],[0.5660430788993835,0.637241005897522],[0.5363480448722839,0.6422877311706543],[0.5135668516159058,0.6395995616912842],[0.5011261701583862,0.6290135383605957],[0.49418604373931885,0.6172480583190918]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.645348846912384,0.5358527302742004],[0.645348846912384,0.5358527302742004],[0.6442641615867615,0.5358527302742004],[0.6426903605461121,0.5358527302742004],[0.6412232518196106,0.5358527302742004],[0.6397576928138733,0.5365989208221436],[0.6370540857315063,0.541783332824707],[0.6306567192077637,0.5543752312660217],[0.6248100399971008,0.5725650191307068],[0.6222805380821228,0.5890226364135742],[0.6234244704246521,0.6030681729316711],[0.627571702003479,0.6072229146957397],[0.6346458196640015,0.610253632068634],[0.6428760886192322,0.6050789952278137],[0.648901641368866,0.5950422883033752],[0.6483222246170044,0.5727021098136902],[0.6412766575813293,0.5521637797355652],[0.6273312568664551,0.5372586250305176],[0.6141592860221863,0.5254628658294678],[0.6057249307632446,0.5213702321052551],[0.6012576222419739,0.5198622941970825],[0.6007751822471619,0.5193798542022705]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.7199612259864807,0.5155038833618164],[0.7199612259864807,0.5155038833618164],[0.719377875328064,0.5155038833618164],[0.7179294228553772,0.5155038833618164],[0.7080923914909363,0.51828932762146],[0.6890599131584167,0.5259787440299988],[0.6713684797286987,0.5347856283187866],[0.6574926376342773,0.5435836911201477],[0.6511395573616028,0.5478191375732422],[0.6478971242904663,0.5507463216781616],[0.6479596495628357,0.5529983639717102],[0.6528645753860474,0.554446816444397],[0.6670903563499451,0.5521843433380127],[0.6791030168533325,0.5454007387161255],[0.6867337822914124,0.5387912392616272],[0.6928579807281494,0.5346342921257019],[0.6986150741577148,0.5346343517303467],[0.7044717073440552,0.5397430658340454],[0.7128928899765015,0.5523818731307983],[0.7205077409744263,0.5631680488586426],[0.7252559065818787,0.5692481994628906],[0.7309309840202332,0.5723453760147095],[0.7350451350212097,0.5674204230308533],[0.739495038986206,0.5480540990829468],[0.7393410801887512,0.5255078673362732],[0.7393410801887512,0.5175310969352722],[0.7402037382125854,0.518410861492157],[0.7443240880966187,0.5237628221511841],[0.7515245676040649,0.542529821395874],[0.7560819387435913,0.5574178695678711],[0.7595565319061279,0.5674294233322144],[0.761091411113739,0.573435366153717],[0.7616279125213623,0.5736433863639832],[0.7616279125213623,0.5722388029098511],[0.7587624788284302,0.564078152179718],[0.755832850933075,0.5524011850357056],[0.757765531539917,0.5405887365341187],[0.7606589198112488,0.5290697813034058],[0.7734351754188538,0.5182314515113831],[0.7965116500854492,0.5038759708404541],[0.8184698820114136,0.49386584758758545],[0.8349595069885254,0.48562097549438477],[0.8453930020332336,0.4793633222579956],[0.850372850894928,0.47714653611183167],[0.851259708404541,0.47625967860221863],[0.8474637269973755,0.47850102186203003],[0.8362277746200562,0.48935362696647644],[0.8194862008094788,0.5045166611671448],[0.8059744834899902,0.5176689028739929],[0.801060140132904,0.5273883938789368],[0.8015190958976746,0.5335957407951355],[0.8150997161865234,0.5355415344238281],[0.8441112041473389,0.5288973450660706]]}\n" +
                "{\"type\":\"pen\",\"color\":0,\"coordinates\":[[0.8294573426246643,0.4079457223415375],[0.8294573426246643,0.4079457223415375],[0.8283336162567139,0.4079457223415375],[0.8275193572044373,0.4199850857257843],[0.8283475637435913,0.4397483170032501],[0.8324477672576904,0.4642973244190216],[0.8386620283126831,0.4900643825531006],[0.8458741903305054,0.5151194930076599],[0.8541727066040039,0.5391666293144226],[0.8628392219543457,0.5611677765846252],[0.8653100728988647,0.5639534592628479]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.3895348906517029,0.9186046719551086],[0.3895348906517029,0.9186046719551086],[0.3883298337459564,0.9186046719551086],[0.38856589794158936,0.9197611212730408],[0.39029762148857117,0.9356747269630432],[0.3973207175731659,0.9670095443725586],[0.4074689745903015,1.0067346096038818],[0.41626715660095215,1.0475541353225708],[0.42221853137016296,1.0783565044403076],[0.42667683959007263,1.1032359600067139],[0.4282580018043518,1.1159497499465942],[0.4297184944152832,1.1233986616134644],[0.42926356196403503,1.123062014579773],[0.42926356196403503,1.121124029159546]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.3672480583190918,0.913759708404541],[0.3672480583190918,0.913759708404541],[0.3672480583190918,0.9126982092857361],[0.3672480583190918,0.9107382297515869],[0.36846286058425903,0.9098837375640869],[0.3780609667301178,0.9098837375640869],[0.4032159149646759,0.9121202826499939],[0.4270268976688385,0.9173187613487244],[0.44459229707717896,0.9238466024398804],[0.4569445550441742,0.9311814308166504],[0.4629773795604706,0.9374911189079285],[0.4664779007434845,0.9471105337142944],[0.4616001546382904,0.9614895582199097],[0.4494991898536682,0.977313220500946],[0.42922472953796387,0.9925170540809631],[0.40574753284454346,1.0025200843811035],[0.3869854509830475,1.003875970840454],[0.374702125787735,0.9988821148872375],[0.36730825901031494,0.9846165180206299],[0.36627906560897827,0.9825581908226013]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.5135658979415894,0.9263566136360168],[0.5135658979415894,0.9263566136360168],[0.5135658979415894,0.9281676411628723],[0.5149717330932617,0.9353236556053162],[0.5183258056640625,0.946407675743103],[0.5212449431419373,0.9566246867179871],[0.5222609639167786,0.961162805557251],[0.5222868323326111,0.9641044735908508],[0.5203391909599304,0.9641472697257996],[0.5169060826301575,0.9621751308441162],[0.5115112662315369,0.9572477340698242],[0.5046698451042175,0.9428324103355408],[0.49983635544776917,0.9257018566131592],[0.4990310072898865,0.9111596345901489],[0.5011252164840698,0.8965058922767639],[0.5108994245529175,0.883480429649353],[0.529353141784668,0.8682020306587219],[0.5494522452354431,0.852949857711792],[0.5532945990562439,0.8498061895370483]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.5436046719551086,0.9127907156944275],[0.5436046719551086,0.9127907156944275],[0.5436046719551086,0.9127907156944275],[0.5574712157249451,0.9103552103042603],[0.5749523639678955,0.8967134952545166],[0.591229259967804,0.8769308924674988],[0.5997776985168457,0.8560761213302612],[0.6032924652099609,0.8423544764518738],[0.6019268035888672,0.8375745415687561],[0.5966368317604065,0.8359324336051941],[0.5837855339050293,0.8456359505653381],[0.5666567087173462,0.8652247786521912],[0.5551978945732117,0.8866972327232361],[0.5489612221717834,0.9031473994255066],[0.5502845644950867,0.9167085289955139],[0.5544482469558716,0.9257569909095764],[0.5679106116294861,0.9327359199523926],[0.5893699526786804,0.9358824491500854],[0.6077693104743958,0.9285860061645508],[0.6239475607872009,0.9198834300041199],[0.6421957015991211,0.9052145481109619],[0.6424418687820435,0.9050387740135193]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.6782945990562439,0.7781007885932922],[0.6782945990562439,0.7781007885932922],[0.6762933731079102,0.7781007885932922],[0.6716180443763733,0.7792210578918457],[0.6621330976486206,0.785353422164917],[0.6487851142883301,0.7990946173667908],[0.637738823890686,0.8152530789375305],[0.6368695497512817,0.8293330669403076],[0.6442736387252808,0.8444612622261047],[0.6646255850791931,0.8595434427261353],[0.6885957717895508,0.8735269904136658],[0.7019608020782471,0.8789087533950806],[0.7089991569519043,0.8832513093948364],[0.707951545715332,0.8873914480209351],[0.7003897428512573,0.8943780660629272],[0.6788337826728821,0.9067071676254272],[0.6552028059959412,0.9149197340011597],[0.6430937647819519,0.9115474224090576],[0.6414728760719299,0.9108527302742004]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.7412790656089783,0.7432170510292053],[0.7412790656089783,0.7432170510292053],[0.7399818301200867,0.7432170510292053],[0.738505482673645,0.7440527677536011],[0.7344300746917725,0.7481281161308289],[0.7200806140899658,0.7633377313613892],[0.7054232358932495,0.7794608473777771],[0.697186291217804,0.7937080264091492],[0.6939864158630371,0.8054496049880981],[0.7020516395568848,0.8144547939300537],[0.7237663269042969,0.8268738985061646],[0.7521770000457764,0.8356407284736633],[0.7742230296134949,0.8430226445198059],[0.7815775275230408,0.8485178351402283],[0.7861242294311523,0.8571321964263916],[0.7784410715103149,0.8696445226669312],[0.7612482309341431,0.8827943801879883],[0.7311640977859497,0.8913519978523254],[0.7073643207550049,0.8914728760719299]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.7955426573753357,0.7848837375640869],[0.7955426573753357,0.7848837375640869],[0.7936334609985352,0.7848837375640869],[0.7921719551086426,0.7848837375640869],[0.7916666865348816,0.783921480178833],[0.7916666865348816,0.7824663519859314],[0.7936310768127441,0.7809944748878479],[0.8033921122550964,0.776104211807251],[0.8231835961341858,0.762223482131958],[0.847242534160614,0.7356681227684021],[0.8742528557777405,0.6973984837532043],[0.8977136611938477,0.649448573589325],[0.9164074063301086,0.5976771712303162],[0.9278964996337891,0.5497726798057556],[0.9370161890983582,0.5014494061470032],[0.9412413239479065,0.45665183663368225],[0.9430176615715027,0.4141167402267456],[0.9398074746131897,0.3723113238811493],[0.9354195594787598,0.33135786652565],[0.9307650923728943,0.2880263328552246],[0.9278310537338257,0.24521677196025848],[0.9265974760055542,0.20563453435897827],[0.9251552224159241,0.1683768928050995],[0.9246375560760498,0.13788042962551117],[0.9244186282157898,0.11377562582492828],[0.9244186282157898,0.09796953201293945],[0.9244186282157898,0.08574020117521286],[0.9234496355056763,0.07841018587350845],[0.9221469759941101,0.07361159473657608],[0.9206943511962891,0.07185712456703186],[0.9205426573753357,0.07037773728370667],[0.9191840291023254,0.07209505885839462]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.9253876209259033,0.06782945990562439],[0.9253876209259033,0.06782945990562439],[0.9253876209259033,0.06782945990562439],[0.9253876209259033,0.0663759708404541],[0.9232929944992065,0.06686046719551086],[0.9198704957962036,0.06978707760572433],[0.9116958975791931,0.07649367302656174],[0.8973054885864258,0.09077400714159012],[0.8827168941497803,0.10682152211666107],[0.8720965385437012,0.11821356415748596],[0.865383505821228,0.12611939013004303],[0.8635905981063843,0.12693798542022705],[0.8633720874786377,0.12693798542022705]]}\n" +
                "{\"type\":\"pen\",\"color\":3,\"coordinates\":[[0.9147287011146545,0.06782945990562439],[0.9147287011146545,0.06782945990562439],[0.9136137962341309,0.06782945990562439],[0.9151883721351624,0.06925816833972931],[0.922450065612793,0.07651980966329575],[0.9366031289100647,0.0892128199338913],[0.9517736434936523,0.10390543192625046],[0.9666770696640015,0.11977782845497131],[0.9791232943534851,0.13222403824329376],[0.9825581908226013,0.13759690523147583]]}";

        String lines[] = initialWhiteboard.split("\\n");
        for (String string : lines) {
            drawingView_canvas.callback(string);
        }
    }

    private class FetchTask extends AsyncTask<Void, Void, String> {
        private String m_retVal = "not changed";
        private Face m_face;
        private boolean m_shouldStop = false;

        @Override
        protected String doInBackground(Void... voids) {
            Log.i("Main", "doInBackground called");
            try {
                m_face = new Face("localhost");
                Log.i("Main", "face created");
                m_face.expressInterest(new Name("/ndn/edu/ucla/remap/ping"),
                        new OnData() {
                            @Override
                            public void
                            onData(Interest interest, Data data) {
                                m_retVal = data.getContent().toString();
                                Log.i("NDN", data.getContent().toHex());
                                m_shouldStop = true;
                            }
                        },
                        new OnTimeout() {
                            @Override
                            public void onTimeout(Interest interest) {
                                m_retVal = "ERROR: Timeout trying";
                                m_shouldStop = true;
                            }
                        });

                while (!m_shouldStop) {
                    m_face.processEvents();
                    //Log.i("Main", "loop");
                    Thread.sleep(500);
                }
                m_face.shutdown();
                m_face = null;
                return m_retVal;
            } catch (Exception e) {
                m_retVal = "ERROR: " + e.getMessage();
                return m_retVal;
            }
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (m_retVal.contains("ERROR:")) {
                new AlertDialog.Builder(WhiteboardActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Error received")
                    .setMessage(m_retVal)
                    .setPositiveButton("OK", null)
                    .show();
            } else {
                Log.i("NDN", m_retVal);
            }
        }

    }
}
