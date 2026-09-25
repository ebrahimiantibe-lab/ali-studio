package com.example.ali;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import java.util.*;

/**
 * Main transparent logo canvas. The 74 presets are rendered as multi-pass metallic
 * materials (extrusion + bevel + body gradient + specular bands + optional brushed
 * anisotropy), rather than as a single flat five-stop color gradient.
 *
 * This is a Canvas-based material renderer, not a physically based GPU/PBR renderer.
 * It deliberately keeps the app dependency-free and works from API 24 upward.
 */
public class LogoCanvas extends View {
    public static class Layer {
        String text; int preset; int tint=0; float x, y, scale=1f, rotation=0f; float depth=8f, bevel=2f, shine=0.75f; int font=0;
        Layer(String t,int p,float xx,float yy){text=t;preset=p;x=xx;y=yy;}
        Layer copy(){Layer n=new Layer(text,preset,x,y);n.tint=tint;n.scale=scale;n.rotation=rotation;n.depth=depth;n.bevel=bevel;n.shine=shine;n.font=font;return n;}
    }
    public static class State {
        ArrayList<Layer> layers=new ArrayList<>(); int selected; boolean shadow,transparent; Uri imageUri;
        State copy(){State s=new State();for(Layer l:layers)s.layers.add(l.copy());s.selected=selected;s.shadow=shadow;s.transparent=transparent;s.imageUri=imageUri;return s;}
    }

    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Layer> layers=new ArrayList<>();
    private Bitmap image; private Uri imageUri;
    private int selected=0; private boolean shadow=true, transparent=true;
    private float lastX,lastY,dist,lastAngle; private boolean moving,zooming,gestureSnap;
    private OnGestureStartListener gestureListener;
    private final Shader[] textureCache=new Shader[74];
    public interface OnGestureStartListener { void onGestureStart(); }
    public void setOnGestureStartListener(OnGestureStartListener l){gestureListener=l;}

    public final String[] names={"طلای خالص","طلای روشن","طلای تیره","طلای قدیمی","طلای ساتن","طلای برس‌خورده","طلای شامپاینی","رزگلد","رزبرنز","برنز کلاسیک","برنز تیره","برنز روشن","مس خالص","مس براق","مس اکسیدشده","مس پتینه","برنج","برنج براق","برنج قدیمی","نقره خالص","نقره براق","نقره مات","نقره ساتن","نقره قدیمی","نقره تیره","کروم روشن","کروم آینه‌ای","کروم تیره","پلاتین","طلای سفید","استیل","استیل آبی","استیل سرد","تیتانیوم","تیتانیوم تیره","گرافیت","گان‌متال","آنتراسیت","قلع","نیکل","روی","آهن","آهن تیره","مشکی متالیک","مشکی کروم","قرمز متالیک","قرمز یاقوتی","قرمز آتشین","قرمز تیره","قرمز کروم","سبز متالیک","سبز زمردی","سبز یشمی","سبز تیره","آبی متالیک","آبی یخی","آبی سلطنتی","آبی تیره","بنفش متالیک","بنفش تیره","صورتی متالیک","صورتی رز","نارنجی متالیک","نارنجی مسی","زرد متالیک","زرد لیمویی","سفید کروم","سفید مرواریدی","خاکستری متالیک","خاکستری براق","قهوه‌ای برنزی","طلایی سبز","طلایی قرمز","نقره آبی"};
    private final int[][] base={{0xFFFFF7B0,0xFFFFD54F,0xFF8A5A00,0xFFFFE38A,0xFFB77A00},{0xFFFFFFFF,0xFFFFD65A,0xFFB77A00,0xFFFFF1A6,0xFFD4AF37},{0xFFFFE9A0,0xFFB8860B,0xFF5C3A00,0xFFD4AF37,0xFF7A4F00},{0xFFE8C56A,0xFF9A6B22,0xFF5E421A,0xFFD1A14A,0xFF76501E},{0xFFFFF0B5,0xFFD4AF37,0xFF9B7621,0xFFE6C65C,0xFF9B7621},{0xFFEFD98A,0xFFB58B2A,0xFFF8E8A6,0xFF8A6A1F,0xFFD1A43C},{0xFFFFE7B8,0xFFE0B36A,0xFFB8865B,0xFFFFE7B8,0xFFC99A5A},{0xFFFFD0C8,0xFFB76E79,0xFF7D3F4B,0xFFE8A0A8,0xFFB76E79},{0xFFE4A18A,0xFFB76E5A,0xFF6E3C2D,0xFFD08B6A,0xFF8F4D39},{0xFFFFD08A,0xFFCD7F32,0xFF6E3B13,0xFFE5A65B,0xFF8E4D1D},{0xFFE0A15A,0xFF8C4A18,0xFF4E2A12,0xFFB96B29,0xFF704016},{0xFFFFD99A,0xFFE2A24A,0xFF9C5E1B,0xFFF4C77A,0xFFC77D28},{0xFFFFD8A5,0xFFB87333,0xFF6D351B,0xFFE7A56B,0xFF8A4622},{0xFFFFE2B7,0xFFDA8A45,0xFF8A4A22,0xFFFFC27D,0xFFB7652F},{0xFFE3B089,0xFFB87333,0xFF5C3828,0xFF7F9B79,0xFFB87333},{0xFFB87333,0xFF6E9C7A,0xFF3F5D4B,0xFFC58A5B,0xFF7A6B4A},{0xFFFFE4A3,0xFFB5A642,0xFF6D6424,0xFFD6C45A,0xFF8C842E},{0xFFFFF0B0,0xFFD4B34C,0xFF7B6A24,0xFFF1D46A,0xFFA18D2D},{0xFFD7C27B,0xFF9B7E31,0xFF5B4A22,0xFFC4A65A,0xFF80662A},{0xFFFFFFFF,0xFFD9DDE2,0xFF777C84,0xFFF8F8F8,0xFF9EA4AC},{0xFFFFFFFF,0xFFE8EAED,0xFF777B82,0xFFFFFFFF,0xFFBFC4CB},{0xFFE8EAED,0xFFB9BEC5,0xFF777C84,0xFFD9DDE2,0xFF8B9097},{0xFFFFFFFF,0xFFD1D5DA,0xFF8E949B,0xFFECEFF2,0xFFB7BCC2},{0xFFD7D9DD,0xFF9B9EA4,0xFF5D6168,0xFFC8CBD0,0xFF7A7E85},{0xFFC8CCD2,0xFF777C84,0xFF3D4147,0xFF9EA4AC,0xFF555A61},{0xFFFFFFFF,0xFFE7EBF0,0xFF6E7680,0xFFFFFFFF,0xFFAEB7C0},{0xFFFFFFFF,0xFFFAFAFA,0xFF8C939C,0xFFFFFFFF,0xFFDCE2E8},{0xFFBFC5CC,0xFF707780,0xFF30343A,0xFF9AA2AB,0xFF454A51},{0xFFFFFFFF,0xFFE5E4E2,0xFF7D7E80,0xFFF8F8F8,0xFFB9B9B9},{0xFFFFFFFF,0xFFF0F0F0,0xFF8C8C8C,0xFFFFFFFF,0xFFCFCFCF},{0xFFF5F7F9,0xFFB8C0C8,0xFF5D6670,0xFFDCE3E9,0xFF8D969F},{0xFFE8F4FF,0xFF8CAFC8,0xFF3B6078,0xFFD6ECFF,0xFF6C8EA8},{0xFFF7FAFF,0xFFB8C9D8,0xFF617384,0xFFDDE9F3,0xFF879BAA},{0xFFF5F7FA,0xFF9EA9B5,0xFF59636E,0xFFD9E1E8,0xFF788490},{0xFFE1E7ED,0xFF8C99A6,0xFF4B5661,0xFFC8D1D9,0xFF66727D},{0xFF6D747B,0xFF394047,0xFF15191D,0xFF7D8790,0xFF252B31},{0xFF737A80,0xFF444B52,0xFF20262B,0xFF929AA2,0xFF30363C},{0xFF6E7378,0xFF33383D,0xFF121619,0xFF81878C,0xFF262B30},{0xFFDCE1E5,0xFF929AA1,0xFF454C52,0xFFEEF2F5,0xFF707980},{0xFFF1F4F6,0xFFB7BEC4,0xFF6B7379,0xFFE4E8EB,0xFF8D959B},{0xFFCDD2D6,0xFF747B80,0xFF34393D,0xFFB4BBC0,0xFF565D62},{0xFF8C9398,0xFF4D5358,0xFF1D2226,0xFF747B80,0xFF30353A},{0xFF777D82,0xFF383D41,0xFF111417,0xFF656B70,0xFF252A2D},{0xFF202428,0xFF090A0B,0xFF5D646A,0xFF111315,0xFF3A3F44},{0xFFEEEEEE,0xFFD0D0D0,0xFF888888,0xFFFFFFFF,0xFFAAAAAA},{0xFFFF7777,0xFFE52D2D,0xFF7A0909,0xFFFFA0A0,0xFFB31313},{0xFFFFB0B0,0xFFD21F2F,0xFF5C0710,0xFFFFD0D0,0xFF9C0E18},{0xFFFF9A7A,0xFFFF3D22,0xFF7D160B,0xFFFFC0A8,0xFFB52B16},{0xFFFF7A7A,0xFFB10F18,0xFF4A0509,0xFFDE3038,0xFF7D0A10},{0xFFFFA0A0,0xFFE51C2A,0xFF5B0006,0xFFFFD0D0,0xFFAA1018},{0xFF9AFFB2,0xFF27AE60,0xFF075B2A,0xFFB8FFD0,0xFF158A48},{0xFFB7FFD0,0xFF20C997,0xFF075D42,0xFFD5FFE4,0xFF159D76},{0xFFB5F2D0,0xFF2E8B57,0xFF0C4A2A,0xFFBFFFE0,0xFF176D45},{0xFF8CD69C,0xFF176B38,0xFF062F18,0xFF59A86E,0xFF0D4D28},{0xFFA6D9FF,0xFF1683D8,0xFF064A82,0xFFD1EEFF,0xFF0E65A9},{0xFFE0F7FF,0xFF71C8FF,0xFF3B789C,0xFFFFFFFF,0xFF73A9C8},{0xFF9FD4FF,0xFF3569D4,0xFF0C2A76,0xFFBFD9FF,0xFF294FA5},{0xFF8BB5FF,0xFF233E8D,0xFF071437,0xFF5B82E6,0xFF182D6A},{0xFFE0B6FF,0xFF8E44AD,0xFF3D145A,0xFFF1D5FF,0xFF6B258A},{0xFFC6A0E8,0xFF5B2C83,0xFF26103B,0xFF9B67C6,0xFF452064},{0xFFFFC5E5,0xFFE84A9A,0xFF7C1551,0xFFFFE0F0,0xFFB52A72},{0xFFFFD1E8,0xFFC65A86,0xFF6E2543,0xFFFFE6F1,0xFF9E3C68},{0xFFFFC28A,0xFFE8751A,0xFF7C3100,0xFFFFD7A8,0xFFB84D0B},{0xFFFFD0A6,0xFFBF6A2C,0xFF6B3210,0xFFFFE4C7,0xFF9A4A1B},{0xFFFFF09A,0xFFF1C40F,0xFF7D5D00,0xFFFFF7C2,0xFFC99F0A},{0xFFFFF3A0,0xFFD4B11F,0xFF766000,0xFFFFFFD0,0xFFB79A0C},{0xFFFFFFFF,0xFFF0F3F6,0xFF9EA7B0,0xFFFFFFFF,0xFFD7DDE2},{0xFFFFFFFF,0xFFE8E4D5,0xFFB4AA8B,0xFFFFF8E0,0xFFD2C8A8},{0xFFE7EBEE,0xFF9AA2AA,0xFF505860,0xFFF7F9FA,0xFF747D85},{0xFFFFFFFF,0xFFC7CDD2,0xFF666F77,0xFFFFFFFF,0xFF9DA5AD},{0xFFD1A06A,0xFF8B5A2B,0xFF4E2C14,0xFFC99560,0xFF71431F},{0xFFF6E17A,0xFF7FA43A,0xFF314D12,0xFFFFEF9A,0xFF9C7B1D},{0xFFFFD07A,0xFFC54A3A,0xFF5C1710,0xFFFFE6A8,0xFF9B2C22},{0xFFF8F8FF,0xFF9AB4D2,0xFF4E5F7A,0xFFFFFFFF,0xFF7289A8}};

    public LogoCanvas(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);layers.add(new Layer("ملک رساوا",0,0,0));}

    public void setImage(Bitmap b){if(image!=null&&image!=b&&!image.isRecycled())image.recycle();image=b;imageUri=null;invalidate();}
    public void setImage(Bitmap b, Uri uri){if(image!=null&&image!=b&&!image.isRecycled())image.recycle();image=b;imageUri=uri;invalidate();}
    public Bitmap getImage(){return image;}
    public Uri getImageUri(){return imageUri;}
    public int getSelected(){return selected;}
    public int count(){return layers.size();}
    public String getText(){return layers.isEmpty()?"":layers.get(selected).text;}
    public void setText(String s){if(layers.isEmpty())addLayer(s);else {layers.get(selected).text=s.length()==0?"لوگو":s;}invalidate();}
    public void addLayer(String s){layers.add(new Layer(s==null||s.length()==0?"لوگو":s,0,0,0));selected=layers.size()-1;invalidate();}
    public void deleteSelected(){if(layers.size()<=1){layers.get(0).text="لوگو";layers.get(0).preset=0;layers.get(0).x=layers.get(0).y=0;layers.get(0).scale=1;layers.get(0).rotation=0;}else{layers.remove(selected);selected=Math.max(0,Math.min(selected,layers.size()-1));}invalidate();}
    public void selectLayer(int i){if(i>=0&&i<layers.size()){selected=i;invalidate();}}
    public void setPreset(int i){layers.get(selected).preset=Math.max(0,Math.min(names.length-1,i));invalidate();}
    public void setShadow(boolean b){shadow=b;invalidate();}
    public boolean hasShadow(){return shadow;}
    public void setTransparent(boolean b){transparent=b;invalidate();}
    public boolean isTransparent(){return transparent;}
    public void resetTransform(){Layer l=layers.get(selected);l.scale=1;l.x=l.y=l.rotation=0;invalidate();}
    public void changeDepth(float d){Layer l=layers.get(selected);l.depth=Math.max(1f,Math.min(28f,d));invalidate();}
    public float getDepth(){return layers.get(selected).depth;}
    public void changeBevel(float d){Layer l=layers.get(selected);l.bevel=Math.max(.5f,Math.min(8f,d));invalidate();}
    public float getBevel(){return layers.get(selected).bevel;}
    public void setShine(float v){layers.get(selected).shine=Math.max(.15f,Math.min(1.5f,v));invalidate();}
    public float getShine(){return layers.get(selected).shine;}
    public void setFont(int f){layers.get(selected).font=Math.max(0,Math.min(11,f));invalidate();}
    public void setTint(int color){layers.get(selected).tint=color;invalidate();}
    public int getTint(){return layers.get(selected).tint;}
    public void clearTint(){layers.get(selected).tint=0;invalidate();}

    public State snapshotState(){State s=new State();for(Layer l:layers)s.layers.add(l.copy());s.selected=selected;s.shadow=shadow;s.transparent=transparent;s.imageUri=imageUri;return s;}
    public void restore(State s){layers.clear();for(Layer l:s.layers)layers.add(l.copy());if(layers.isEmpty())layers.add(new Layer("لوگو",0,0,0));selected=Math.max(0,Math.min(s.selected,layers.size()-1));shadow=s.shadow;transparent=s.transparent;imageUri=s.imageUri;invalidate();}

    private void checker(Canvas c){int z=28;Paint q=new Paint();for(int y=0;y<getHeight();y+=z)for(int x=0;x<getWidth();x+=z){q.setColor(((x/z+y/z)&1)==0?0xFF303030:0xFF242424);c.drawRect(x,y,x+z,y+z,q);}}
    @Override protected void onDraw(Canvas c){
        if(transparent)checker(c);else c.drawColor(0xFF151515);
        if(image!=null){Paint ip=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);float mw=getWidth()*.72f,mh=getHeight()*.72f;float sc=Math.min(mw/image.getWidth(),mh/image.getHeight());float iw=image.getWidth()*sc,ih=image.getHeight()*sc;c.drawBitmap(image,null,new RectF((getWidth()-iw)/2f,(getHeight()-ih)/2f,(getWidth()+iw)/2f,(getHeight()+ih)/2f),ip);}
        for(int i=0;i<layers.size();i++)drawLayer(c,layers.get(i),i==selected,false);
    }

    private boolean isBrushed(String n){return n.contains("برس")||n.contains("ساتن")||n.contains("استیل");}
    private boolean isMirror(String n){return n.contains("کروم")||n.contains("آینه")||n.contains("براق");}
    private boolean isMatte(String n){return n.contains("مات")||n.contains("تیره");}

    private Shader microTexture(int idx,float size,boolean brushed){
        if(idx>=0&&idx<textureCache.length&&textureCache[idx]!=null)return textureCache[idx];
        int n=96; Bitmap b=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888); int[] px=new int[n*n];
        long seed=0x9E3779B97F4A7C15L ^ (idx*0xBF58476D1CE4E5B9L);
        for(int y=0;y<n;y++) for(int x=0;x<n;x++){
            seed ^= (seed<<13); seed ^= (seed>>>7); seed ^= (seed<<17);
            int noise=(int)(seed&31L)-16;
            int line=brushed ? ((y*13 + x*3 + idx*7)&15)-8 : ((x*7+y*11+idx)&31)-16;
            int v=Math.max(0,Math.min(255,168+noise+line));
            px[y*n+x]=Color.argb(105,v,v,v);
        }
        b.setPixels(px,0,n,0,0,n,n);
        Shader sh=new BitmapShader(b,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT); if(idx>=0&&idx<textureCache.length)textureCache[idx]=sh; return sh;
    }

    private void drawLayer(Canvas c,Layer l,boolean sel,boolean export){
        c.save();float w=c.getWidth(),h=c.getHeight();float sx=export?w/Math.max(1,getWidth()):1;
        c.translate(w/2f+l.x*sx,h/2f+l.y*sx);c.rotate(l.rotation);c.scale(l.scale,l.scale);
        float size=Math.min(w,h)*.16f;
        p.setTypeface(typefaceFor(l.font));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);
        int idx=Math.max(0,Math.min(l.preset,names.length-1)); String mat=names[idx]; int[] raw=base[idx]; int[] col=new int[5]; for(int ci=0;ci<5;ci++) col[ci]=tintColor(raw[ci],l.tint);
        float depth=Math.max(1f,l.depth + (mat.contains("کروم")||mat.contains("آینه")?2f:0f));
        float bevel=Math.max(.5f,l.bevel + (isMirror(mat)?0.8f:0f));

        // 1) soft cast shadow
        if(shadow){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xCC000000);p.setShadowLayer(size*.11f,size*.035f,size*.05f,0xAA000000);c.drawText(l.text,0,size*.35f,p);p.clearShadowLayer();}

        // 2) real-looking extrusion: several offset passes with progressively darker metal
        for(int i=(int)depth;i>=1;i--){
            float f=i/depth; p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,-size*.9f,0,size*.9f,
                    new int[]{shade(col[2],0.70f+0.20f*f),shade(col[2],0.48f),shade(col[4],0.62f)},null,Shader.TileMode.CLAMP));
            c.drawText(l.text,i*.72f,i*.72f+size*.35f,p);
        }
        p.setShader(null);

        // 3) body with vertical metal response: dark-to-light-to-dark transitions
        int[] body={shade(col[2],isMatte(mat)?.80f:.62f),col[1],col[0],col[3],shade(col[2],isMatte(mat)?.90f:.70f),col[4]};
        float[] pos={0f,.20f,.38f,.52f,.72f,1f};
        Shader bodyShader=new LinearGradient(0,-size,0,size,body,pos,Shader.TileMode.CLAMP);
        Shader tex=microTexture(idx,size,isBrushed(mat));
        p.setShader(new ComposeShader(bodyShader,tex,PorterDuff.Mode.MULTIPLY));
        c.drawText(l.text,0,size*.35f,p);p.setShader(null);

        // 4) bevel edge: bright top/left edge and darker lower edge
        stroke.setTypeface(p.getTypeface());stroke.setTextSize(size);stroke.setTextAlign(Paint.Align.CENTER);stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(bevel*1.5f);stroke.setColor(0x66FFFFFF);c.drawText(l.text,-.45f,-.35f+size*.35f,stroke);
        stroke.setStrokeWidth(bevel);stroke.setColor(0x66000000);c.drawText(l.text,.45f,.55f+size*.35f,stroke);

        // 5) specular bands; mirror metals get sharper bands, matte metals get softer bands
        int alpha=(int)(255f*Math.max(.15f,Math.min(1f,l.shine))*(isMirror(mat)?0.68f:0.42f));
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0,-size*.95f,0,size*.05f,new int[]{0x00FFFFFF,alpha<<24|0xFFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
        c.drawText(l.text,0,size*.35f,p);p.setShader(null);
        if(isMirror(mat)){
            p.setShader(new LinearGradient(-size,0,size,0,new int[]{0x00FFFFFF,0x80FFFFFF,0x00FFFFFF},null,Shader.TileMode.CLAMP));
            c.drawText(l.text,0,size*.35f,p);p.setShader(null);
        }
        // 6) brushed anisotropic highlight: thin diagonal/vertical strokes clipped by text mask
        if(isBrushed(mat)){
            p.setShader(new LinearGradient(0,-size,0,size,new int[]{0x10FFFFFF,0x55FFFFFF,0x10FFFFFF},null,Shader.TileMode.MIRROR));
            p.setAlpha(180);c.drawText(l.text,0,size*.35f,p);p.setAlpha(255);p.setShader(null);
        }
        if(sel&&!export){stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(2);stroke.setColor(0x88FFFFFF);c.drawRect(-w*.42f,-size*.75f,w*.42f,size*.65f,stroke);}
        c.restore();
    }

    private Typeface typefaceFor(int id){
        // System families are available without bundling large font files and have Arabic/Persian fallback.
        switch(id){
            case 1: return Typeface.create("sans-serif",Typeface.NORMAL);
            case 2: return Typeface.create("sans-serif",Typeface.BOLD);
            case 3: return Typeface.create("sans-serif-condensed",Typeface.BOLD);
            case 4: return Typeface.create("sans-serif-medium",Typeface.NORMAL);
            case 5: return Typeface.create("sans-serif-light",Typeface.NORMAL);
            case 6: return Typeface.create("serif",Typeface.NORMAL);
            case 7: return Typeface.create("serif",Typeface.BOLD);
            case 8: return Typeface.create("monospace",Typeface.BOLD);
            case 9: return Typeface.create("sans-serif-black",Typeface.NORMAL);
            case 10: return Typeface.create("sans-serif-condensed",Typeface.NORMAL);
            case 11: return Typeface.create("sans-serif",Typeface.ITALIC);
            default: return Typeface.create("sans-serif",Typeface.BOLD);
        }
    }

    private int tintColor(int original,int tint){
        if(tint==0)return original;
        float amount=0.30f;
        int r=(int)(Color.red(original)*(1-amount)+Color.red(tint)*amount);
        int g=(int)(Color.green(original)*(1-amount)+Color.green(tint)*amount);
        int b=(int)(Color.blue(original)*(1-amount)+Color.blue(tint)*amount);
        return Color.argb(Color.alpha(original),r,g,b);
    }

    private int shade(int color,float factor){int a=Color.alpha(color);int r=Math.min(255,Math.max(0,(int)(Color.red(color)*factor)));int g=Math.min(255,Math.max(0,(int)(Color.green(color)*factor)));int b=Math.min(255,Math.max(0,(int)(Color.blue(color)*factor)));return Color.argb(a,r,g,b);}
    public Bitmap exportBitmap(int size){Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);if(transparent)c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);else c.drawColor(0xFF151515);if(image!=null){Paint ip=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);float sc=Math.min(size*.72f/image.getWidth(),size*.72f/image.getHeight());float iw=image.getWidth()*sc,ih=image.getHeight()*sc;c.drawBitmap(image,null,new RectF((size-iw)/2f,(size-ih)/2f,(size+iw)/2f,(size+ih)/2f),ip);}for(Layer l:layers)drawLayer(c,l,false,true);return b;}
    private float spacing(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return(float)Math.hypot(x,y);}
    private float angle(MotionEvent e){return(float)Math.toDegrees(Math.atan2(e.getY(1)-e.getY(0),e.getX(1)-e.getX(0)));}
    @Override public boolean onTouchEvent(MotionEvent e){if(layers.isEmpty())return true;Layer l=layers.get(selected);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:if(!gestureSnap){gestureSnap=true;if(gestureListener!=null)gestureListener.onGestureStart();}moving=true;lastX=e.getX();lastY=e.getY();return true;case MotionEvent.ACTION_POINTER_DOWN:if(e.getPointerCount()==2){zooming=true;dist=spacing(e);lastAngle=angle(e);}return true;case MotionEvent.ACTION_MOVE:if(zooming&&e.getPointerCount()>=2){float n=spacing(e);if(dist>0)l.scale=Math.max(.25f,Math.min(5f,l.scale*n/dist));float a=angle(e),d=a-lastAngle;if(d>180)d-=360;if(d<-180)d+=360;l.rotation+=d;lastAngle=a;dist=n;invalidate();}else if(moving){l.x+=e.getX()-lastX;l.y+=e.getY()-lastY;lastX=e.getX();lastY=e.getY();invalidate();}return true;case MotionEvent.ACTION_POINTER_UP:zooming=false;return true;case MotionEvent.ACTION_UP:moving=false;zooming=false;gestureSnap=false;return true;case MotionEvent.ACTION_CANCEL:moving=false;zooming=false;gestureSnap=false;return true;}return true;}
}
