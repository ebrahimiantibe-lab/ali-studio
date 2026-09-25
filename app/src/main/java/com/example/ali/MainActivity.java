package com.example.ali;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LogoCanvas canvas; EditText input; LinearLayout root,layerBar; HorizontalScrollView layerScroll;
    final ArrayList<LogoCanvas.State> undo=new ArrayList<>(), redo=new ArrayList<>();
    static final int REQ_IMAGE=10, REQ_SAVE_PROJECT=11, REQ_OPEN_PROJECT=12;
    int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(z);t.setGravity(Gravity.CENTER);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);showAd();}

    void showAd(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setPadding(dp(24),dp(24),dp(24),dp(24));r.setBackgroundColor(0xFF101010);
        ImageView im=new ImageView(this);im.setImageResource(R.drawable.ad_circle);im.setScaleType(ImageView.ScaleType.CENTER_CROP);r.addView(im,new LinearLayout.LayoutParams(dp(170),dp(170)));
        TextView tx=tv("جان فدای پسر تیم آقاجان",20);tx.setPadding(0,dp(12),0,dp(16));r.addView(tx);
        TextView ch=tv("کانال ali در ایتا\nبرای دنبال‌کردن کانال روی دکمه زیر بزنید",16);r.addView(ch,new LinearLayout.LayoutParams(-1,dp(70)));
        Button follow=btn("دنبال کردن");r.addView(follow,new LinearLayout.LayoutParams(-1,dp(55)));
        Button close=btn("بستن");r.addView(close,new LinearLayout.LayoutParams(-1,dp(55)));
        follow.setOnClickListener(v->openOfficialEitaa());
        close.setOnClickListener(v->buildEditor());setContentView(r);
    }

    private void openOfficialEitaa(){
        Uri uri=Uri.parse("https://eitaa.com/ali12536");
        String scheme=uri.getScheme();
        String host=uri.getHost();
        if(!"https".equalsIgnoreCase(scheme)||host==null||!"eitaa.com".equalsIgnoreCase(host)){
            Toast.makeText(this,"نشانی تأیید نشد",Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW,uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        }catch(ActivityNotFoundException ex){
            Toast.makeText(this,"مرورگر مناسب پیدا نشد",Toast.LENGTH_SHORT).show();
        }
    }

    void buildEditor(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFF101010);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        Button undoB=btn("↩"),redoB=btn("↪"),gallery=btn("گالری"),savePng=btn("PNG"),saveProj=btn("ذخیره"),open=btn("بازکردن");
        top.addView(undoB,new LinearLayout.LayoutParams(dp(48),dp(55)));top.addView(redoB,new LinearLayout.LayoutParams(dp(48),dp(55)));
        top.addView(tv("ali Studio",20),new LinearLayout.LayoutParams(0,dp(55),1));
        top.addView(open,new LinearLayout.LayoutParams(dp(78),dp(55)));top.addView(saveProj,new LinearLayout.LayoutParams(dp(68),dp(55)));top.addView(gallery,new LinearLayout.LayoutParams(dp(70),dp(55)));top.addView(savePng,new LinearLayout.LayoutParams(dp(58),dp(55)));root.addView(top);
        canvas=new LogoCanvas(this);root.addView(canvas,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout textRow=new LinearLayout(this);input=new EditText(this);input.setText("ملک رساوا");input.setTextColor(Color.WHITE);input.setHintTextColor(0xFFAAAAAA);input.setTextSize(18);input.setSingleLine(true);input.setGravity(Gravity.RIGHT);input.setBackgroundColor(0xFF252525);Button apply=btn("متن");textRow.addView(input,new LinearLayout.LayoutParams(0,dp(52),1));textRow.addView(apply,new LinearLayout.LayoutParams(dp(70),dp(52)));root.addView(textRow);

        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout presets=new LinearLayout(this);presets.setPadding(dp(4),dp(3),dp(4),dp(3));
        for(int i=0;i<canvas.names.length;i++){final int k=i;Button q=btn(canvas.names[i]);q.setTextSize(12);q.setOnClickListener(v->{snap();canvas.setPreset(k);});presets.addView(q,new LinearLayout.LayoutParams(dp(115),dp(48)));}
        hs.addView(presets);root.addView(hs,new LinearLayout.LayoutParams(-1,dp(55)));

        HorizontalScrollView toolScroll=new HorizontalScrollView(this);LinearLayout tools=new LinearLayout(this);
        Button add=btn("+ لایه"),del=btn("حذف لایه"),shadow=btn("سایه"),bg=btn("شفاف"),reset=btn("↺ تنظیم"),depth=btn("عمق"),bevel=btn("لبه"),shine=btn("درخشش"),font=btn("فونت"),metalColor=btn("رنگ فلز");
        tools.addView(add,new LinearLayout.LayoutParams(dp(100),dp(52)));tools.addView(del,new LinearLayout.LayoutParams(dp(110),dp(52)));tools.addView(shadow,new LinearLayout.LayoutParams(dp(85),dp(52)));tools.addView(bg,new LinearLayout.LayoutParams(dp(85),dp(52)));tools.addView(reset,new LinearLayout.LayoutParams(dp(105),dp(52)));tools.addView(depth,new LinearLayout.LayoutParams(dp(80),dp(52)));tools.addView(bevel,new LinearLayout.LayoutParams(dp(80),dp(52)));tools.addView(shine,new LinearLayout.LayoutParams(dp(90),dp(52)));tools.addView(font,new LinearLayout.LayoutParams(dp(80),dp(52)));tools.addView(metalColor,new LinearLayout.LayoutParams(dp(95),dp(52)));
        toolScroll.addView(tools);root.addView(toolScroll,new LinearLayout.LayoutParams(-1,dp(58)));
        layerScroll=new HorizontalScrollView(this);layerBar=new LinearLayout(this);layerBar.setPadding(dp(4),dp(2),dp(4),dp(2));layerScroll.addView(layerBar);root.addView(layerScroll,new LinearLayout.LayoutParams(-1,dp(48)));

        apply.setOnClickListener(v->{snap();canvas.setText(input.getText().toString());});
        add.setOnClickListener(v->{snap();canvas.addLayer(input.getText().toString());refreshLayerButtons();});
        del.setOnClickListener(v->{snap();canvas.deleteSelected();refreshLayerButtons();});
        shadow.setOnClickListener(v->{snap();canvas.setShadow(!canvas.hasShadow());});
        bg.setOnClickListener(v->{snap();canvas.setTransparent(!canvas.isTransparent());});
        reset.setOnClickListener(v->{snap();canvas.resetTransform();});metalColor.setOnClickListener(v->metalColorDialog());depth.setOnClickListener(v->sliderDialog("عمق سه‌بعدی",1,28,canvas.getDepth(),v2->{snap();canvas.changeDepth(v2);}));bevel.setOnClickListener(v->sliderDialog("لبه و بِل",1,80,canvas.getBevel()*10f,v2->{snap();canvas.changeBevel(v2/10f);}));shine.setOnClickListener(v->sliderDialog("شدت درخشش",15,150,canvas.getShine()*100f,v2->{snap();canvas.setShine(v2/100f);}));font.setOnClickListener(v->fontDialog());
        undoB.setOnClickListener(v->doUndo());redoB.setOnClickListener(v->doRedo());gallery.setOnClickListener(v->pickImage());savePng.setOnClickListener(v->savePng());saveProj.setOnClickListener(v->saveProjectDialog());open.setOnClickListener(v->openProject());
        setContentView(root);
        canvas.setOnGestureStartListener(()->snap());
        refreshLayerButtons();
    }

    interface FloatApply { void apply(float v); }
    void sliderDialog(String title,int min,int max,float value,FloatApply apply){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(8),dp(20),dp(8));
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,Math.round(value-min))));
        TextView val=tv(String.valueOf(Math.round(value)),16);box.addView(val);box.addView(bar);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(box).setPositiveButton("اعمال",null).setNegativeButton("لغو",null).create();
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){val.setText(String.valueOf(min+p));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{apply.apply(min+bar.getProgress());d.dismiss();}));d.show();
    }
    void fontDialog(){
        String[] fs={"فارسی ضخیم","فارسی ساده","فارسی پررنگ","فشرده پررنگ","Medium","Light","Serif","Serif Bold","Monospace","Black","Condensed","Italic"};
        new AlertDialog.Builder(this).setTitle("فونت متن").setItems(fs,(d,w)->{snap();canvas.setFont(w);}).show();
    }
    void metalColorDialog(){
        final String[] names={"بدون ته‌رنگ","طلایی","نقره‌ای","کروم","مسی","برنزی","رزگلد","قرمز","سبز","آبی","بنفش","مشکی","سفید","تیتانیومی"};
        final int[] colors={0,0xFFD4AF37,0xFFD9DDE2,0xFFF5F7F9,0xFFB87333,0xFFCD7F32,0xFFB76E79,0xFFE52D2D,0xFF20C997,0xFF1683D8,0xFF8E44AD,0xFF202428,0xFFFFFFFF,0xFF9EA9B5};
        new AlertDialog.Builder(this).setTitle("رنگ پایه فلز").setItems(names,(d,w)->{snap();if(w==0)canvas.clearTint();else canvas.setTint(colors[w]);}).show();
    }
    void refreshLayerButtons(){
        if(layerBar==null||canvas==null)return;layerBar.removeAllViews();
        for(int i=0;i<canvas.count();i++){final int k=i;Button b=btn("لایه "+(i+1));b.setTextSize(12);b.setOnClickListener(v->{canvas.selectLayer(k);input.setText(canvas.getText());refreshLayerButtons();});if(i==canvas.getSelected())b.setText("● لایه "+(i+1));layerBar.addView(b,new LinearLayout.LayoutParams(dp(90),dp(42)));}
    }

    void snap(){if(canvas==null)return;undo.add(canvas.snapshotState());if(undo.size()>40)undo.remove(0);redo.clear();}
    void doUndo(){if(canvas==null||undo.isEmpty())return;redo.add(canvas.snapshotState());LogoCanvas.State s=undo.remove(undo.size()-1);restoreState(s);input.setText(canvas.getText());refreshLayerButtons();}
    void doRedo(){if(canvas==null||redo.isEmpty())return;undo.add(canvas.snapshotState());LogoCanvas.State s=redo.remove(redo.size()-1);restoreState(s);input.setText(canvas.getText());refreshLayerButtons();}
    void restoreState(LogoCanvas.State s){canvas.restore(s);if(s.imageUri!=null)loadBitmapFromUri(s.imageUri,true);else canvas.setImage((Bitmap)null);}

    void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,REQ_IMAGE);}
    void openProject(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/octet-stream");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,REQ_OPEN_PROJECT);}
    void saveProjectDialog(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"ali_project.aliproj");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/octet-stream","application/json"});startActivityForResult(i,REQ_SAVE_PROJECT);}

    void loadBitmapFromUri(Uri uri,boolean silent){try{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream a=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(a,null,o);}int max=4096,sw=Math.max(1,o.outWidth),sh=Math.max(1,o.outHeight),sample=1;while(sw/sample>max||sh/sample>max)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap b;try(InputStream a=getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(a,null,o);}if(b==null)throw new IOException();canvas.setImage(b,uri);}catch(Exception e){if(!silent)Toast.makeText(this,"تصویر باز نشد",Toast.LENGTH_SHORT).show();}}

    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null||d.getData()==null)return;Uri u=d.getData();
        try{if(r==REQ_IMAGE){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}snap();loadBitmapFromUri(u,false);}
        else if(r==REQ_SAVE_PROJECT){writeProject(u);}
        else if(r==REQ_OPEN_PROJECT){try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}readProject(u);}}catch(Exception e){Toast.makeText(this,"عملیات انجام نشد",Toast.LENGTH_LONG).show();}}

    JSONObject stateToJson(){JSONObject o=new JSONObject();try{o.put("version",1);o.put("selected",canvas.getSelected());o.put("shadow",canvas.hasShadow());o.put("transparent",canvas.isTransparent());o.put("imageUri",canvas.getImageUri()==null?JSONObject.NULL:canvas.getImageUri().toString());JSONArray a=new JSONArray();for(LogoCanvas.Layer l:canvas.snapshotState().layers){JSONObject x=new JSONObject();x.put("text",l.text);x.put("preset",l.preset);x.put("tint",l.tint);x.put("x",l.x);x.put("y",l.y);x.put("scale",l.scale);x.put("rotation",l.rotation);x.put("depth",l.depth);x.put("bevel",l.bevel);x.put("shine",l.shine);x.put("font",l.font);a.put(x);}o.put("layers",a);}catch(Exception ignored){}return o;}
    void writeProject(Uri u){try{OutputStream out=getContentResolver().openOutputStream(u);if(out==null)throw new IOException();out.write(stateToJson().toString(2).getBytes("UTF-8"));out.close();Toast.makeText(this,"پروژه ذخیره شد ✓",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"خطا در ذخیره پروژه",Toast.LENGTH_LONG).show();}}
    void readProject(Uri u){try{InputStream in=getContentResolver().openInputStream(u);if(in==null)throw new IOException();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)b.write(buf,0,n);in.close();JSONObject o=new JSONObject(new String(b.toByteArray(),"UTF-8"));JSONArray a=o.optJSONArray("layers");LogoCanvas.State s=new LogoCanvas.State();if(a!=null)for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);LogoCanvas.Layer l=new LogoCanvas.Layer(x.optString("text","لوگو"),x.optInt("preset",0),(float)x.optDouble("x",0),(float)x.optDouble("y",0));l.tint=x.optInt("tint",0);l.scale=(float)x.optDouble("scale",1);l.rotation=(float)x.optDouble("rotation",0);l.depth=(float)x.optDouble("depth",8);l.bevel=(float)x.optDouble("bevel",2);l.shine=(float)x.optDouble("shine",.75);l.font=x.optInt("font",0);s.layers.add(l);}if(s.layers.isEmpty())s.layers.add(new LogoCanvas.Layer("لوگو",0,0,0));s.selected=o.optInt("selected",0);s.shadow=o.optBoolean("shadow",true);s.transparent=o.optBoolean("transparent",true);String iu=o.optString("imageUri","");s.imageUri=iu.length()==0?null:Uri.parse(iu);snap();restoreState(s);input.setText(canvas.getText());refreshLayerButtons();Toast.makeText(this,"پروژه باز شد ✓",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"فایل پروژه معتبر نیست",Toast.LENGTH_LONG).show();}}

    void savePng(){Bitmap b=null;Uri u=null;try{b=canvas.exportBitmap(2048);String name="ali_"+System.currentTimeMillis()+".png";ContentValues v=new ContentValues();v.put(MediaStore.Images.Media.DISPLAY_NAME,name);v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");if(Build.VERSION.SDK_INT>=29){v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/ali");v.put(MediaStore.Images.Media.IS_PENDING,1);}u=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IOException();try(OutputStream o=getContentResolver().openOutputStream(u)){if(o==null||!b.compress(Bitmap.CompressFormat.PNG,100,o))throw new IOException();}if(Build.VERSION.SDK_INT>=29){ContentValues done=new ContentValues();done.put(MediaStore.Images.Media.IS_PENDING,0);getContentResolver().update(u,done,null,null);}Toast.makeText(this,"PNG شفاف 2048 ذخیره شد ✓",Toast.LENGTH_LONG).show();}catch(Exception e){if(u!=null)try{getContentResolver().delete(u,null,null);}catch(Exception ignored){}Toast.makeText(this,"خطا در ذخیره PNG",Toast.LENGTH_LONG).show();}finally{if(b!=null&&!b.isRecycled())b.recycle();}}
}
