package info.nightscout.androidaps.plugins.general.autotune.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;

import dagger.android.HasAndroidInjector;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.StaticInjector;
import info.nightscout.androidaps.interfaces.ActivePluginProvider;
import info.nightscout.androidaps.interfaces.InsulinInterface;
import info.nightscout.androidaps.utils.SafeParse;
import info.nightscout.androidaps.utils.resources.ResourceHelper;
import info.nightscout.androidaps.utils.sharedPreferences.SP;

public class TunedProfile  {
    private Profile profile;
    public String profilename;
    private Profile.ProfileValue pv;
    private static List basalsResult = new ArrayList<>();
    public double currentBasal;
    @Inject ActivePluginProvider activePlugin;
    @Inject SP sp;
    @Inject ResourceHelper resourceHelper;
    private final HasAndroidInjector injector;


    public TunedProfile (Profile profile) {
        injector = StaticInjector.Companion.getInstance();
        injector.androidInjector().inject(this);
        this.profile=profile;
    }

    public void setIC(double ic) {

    }

    public void setISF(double isf) {

    }

    public double getAvgISF() {
        return profile.getIsfsMgdl().length==1?profile.getIsfsMgdl()[0].value:averageProfileValue(profile.getIsfsMgdl());
    }

    public double getAvgIC() {
        return profile.getIcs().length==1?profile.getIcs()[0].value:averageProfileValue(profile.getIcs());
    }

    public Profile getProfile() {
        //todo add code to update profile with basal data, ISF and CR


        return profile;
    }

    public static double averageProfileValue(Profile.ProfileValue[] pf) {
        double avgValue = 0;
        int secondPerDay=24*60*60;
        if (pf == null)
            return avgValue;
        for(int i = 0; i< pf.length;i++) {
            avgValue+=pf[i].value*((i==pf.length -1 ? secondPerDay : pf[i+1].timeAsSeconds) -pf[i].timeAsSeconds);
        }
        avgValue/=secondPerDay;
        return avgValue;
    }

    public JSONObject profiletoOrefJSON()  {
        // Create a json profile with oref0 format
        // Include min_5m_carbimpact, insulin type, single value for carb_ratio and isf
        JSONObject json = new JSONObject();
        JSONObject store = new JSONObject();
        JSONObject convertedProfile = new JSONObject();
        int basalIncrement = 60 ;
        InsulinInterface insulinInterface = activePlugin.getActiveInsulin();

        try {
            json.put("name",profilename);
            json.put("min_5m_carbimpact",sp.getDouble("openapsama_min_5m_carbimpact", 3.0));
            json.put("dia", profile.getDia());

            JSONArray basals = new JSONArray();
            for (int h = 0; h < 24; h++) {
                int secondfrommidnight = h * 60 * 60;
                String time;
                time = (h<10 ? "0"+ h : h)  + ":00:00";
                //basals.put(new JSONObject().put("start", time).put("minutes", h * basalIncrement).put("rate", getProfileBasal(h)));
                basals.put(new JSONObject().put("start", time).put("minutes", h * basalIncrement).put("rate", profile.getBasalTimeFromMidnight(secondfrommidnight)));
            };
            json.put("basalprofile", basals);
            int isfvalue = (int) profile.getIsfMgdl();
            json.put("isfProfile",new JSONObject().put("sensitivities",new JSONArray().put(new JSONObject().put("i",0).put("start","00:00:00").put("sensitivity",isfvalue).put("offset",0).put("x",0).put("endoffset",1440))));
            // json.put("carbratio", new JSONArray().put(new JSONObject().put("time", "00:00").put("timeAsSeconds", 0).put("value", previousResult.optDouble("carb_ratio", 0d))));
            json.put("carb_ratio", profile.getIc());
            json.put("autosens_max", SafeParse.stringToDouble(sp.getString(R.string.key_openapsama_autosens_max, "1.2")));
            json.put("autosens_min", SafeParse.stringToDouble(sp.getString(R.string.key_openapsama_autosens_min, "0.7")));
            json.put("units",sp.getString(R.string.key_units, "mg/dl"));
            json.put("timezone", TimeZone.getDefault().getID());
            if (insulinInterface.getId() == InsulinInterface.OREF_ULTRA_RAPID_ACTING)
                json.put("curve","ultra-rapid");
            else if (insulinInterface.getId() == InsulinInterface.OREF_RAPID_ACTING)
                json.put("curve","rapid-acting");
            else if (insulinInterface.getId() == InsulinInterface.OREF_FREE_PEAK) {
                json.put("curve", "bilinear");
                json.put("insulinpeaktime",sp.getInt(resourceHelper.gs(R.string.key_insulin_oref_peak),75));
            }

        } catch (JSONException e) {}

        return json;
    }

    public static void basalsResultInit(){
        // initialize basalsResult if
//        log.debug(" basalsResult init");
        if(basalsResult.isEmpty()) {
            for (int i = 0; i < 24; i++) {
                basalsResult.add(0d);
            }
        } else {
            for (int i = 0; i < 24; i++) {
                basalsResult.set(i, 0d);
            }
        }
    }

}
