package com.vr.asearchview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    /*
    Our data object
     */
    public class Spacecraft {
        /*
        INSTANCE FIELDS
         */
        private int id;
        private String name;
        private String propellant;
        private String imageURL;
        private int technologyExists;
        /*
        GETTERS AND SETTERS
         */
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getPropellant() {
            return propellant;
        }
        public void setPropellant(String propellant) {
            this.propellant = propellant;
        }
        public String getImageURL() {
            return imageURL;
        }
        public void setImageURL(String imageURL) {
            this.imageURL = imageURL;
        }
        public int getTechnologyExists() {
            return technologyExists;
        }
        public void setTechnologyExists(int technologyExists) {
            this.technologyExists = technologyExists;
        }
        /*
        TOSTRING
         */
        @Override
        public String toString() {
            return name;
        }
    }
    class FilterHelper extends Filter {
        ArrayList<Spacecraft> currentList;
        ListViewAdapter adapter;
        Context c;

        public FilterHelper(ArrayList<Spacecraft> currentList, ListViewAdapter adapter,Context c) {
            this.currentList = currentList;
            this.adapter = adapter;
            this.c=c;
        }
        /*
        - Perform actual filtering.
         */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults=new FilterResults();

            if(constraint != null && constraint.length()>0)
            {
                //CHANGE TO UPPER
                constraint=constraint.toString().toUpperCase();

                //HOLD FILTERS WE FIND
                ArrayList<Spacecraft> foundFilters=new ArrayList<>();

                Spacecraft spacecraft=null;

                //ITERATE CURRENT LIST
                for (int i=0;i<currentList.size();i++)
                {
                    spacecraft= currentList.get(i);

                    //SEARCH
                    if(spacecraft.getName().toUpperCase().contains(constraint) )
                    {
                        //ADD IF FOUND
                        foundFilters.add(spacecraft);
                    }
                }

                //SET RESULTS TO FILTER LIST
                filterResults.count=foundFilters.size();
                filterResults.values=foundFilters;
            }else
            {
                //NO ITEM FOUND.LIST REMAINS INTACT
                filterResults.count=currentList.size();
                filterResults.values=currentList;
            }

            //RETURN RESULTS
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            adapter.setSpacecrafts((ArrayList<Spacecraft>) filterResults.values);
            adapter.refresh();
        }
    }

    /*
    Our custom adapter class
     */
    public class ListViewAdapter extends BaseAdapter implements Filterable {

        Context c;
        ArrayList<Spacecraft> spacecrafts;
        public ArrayList<Spacecraft> currentList;
        FilterHelper filterHelper;

        public ListViewAdapter(Context c, ArrayList<Spacecraft> spacecrafts) {
            this.c = c;
            this.spacecrafts = spacecrafts;
            this.currentList=spacecrafts;
        }
        @Override
        public int getCount() {
            return spacecrafts.size();
        }
        @Override
        public Object getItem(int i) {
            return spacecrafts.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null)
            {
                view= LayoutInflater.from(c).inflate(R.layout.model,viewGroup,false);
            }

            TextView txtName = view.findViewById(R.id.nameTextView);
            TextView txtPropellant = view.findViewById(R.id.propellantTextView);
            CheckBox chkTechExists = view.findViewById(R.id.myCheckBox);
            ImageView spacecraftImageView = view.findViewById(R.id.spacecraftImageView);

            final Spacecraft s= (Spacecraft) this.getItem(i);

            txtName.setText(s.getName());
            txtPropellant.setText(s.getPropellant());
            //chkTechExists.setEnabled(true);
            chkTechExists.setChecked( s.getTechnologyExists()==1);
            chkTechExists.setEnabled(false);

            if(s.getImageURL() != null && s.getImageURL().length()>0)
            {
                Picasso.get().load(s.getImageURL()).placeholder(R.mipmap.ic_launcher).into(spacecraftImageView);
            }else {
                Toast.makeText(c, "Empty Image URL", Toast.LENGTH_LONG).show();
                Picasso.get().load(R.mipmap.ic_launcher).into(spacecraftImageView);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(c, s.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            return view;
        }
        public void setSpacecrafts(ArrayList<Spacecraft> filteredSpacecrafts)
        {
            this.spacecrafts=filteredSpacecrafts;

        }
        @Override
        public Filter getFilter() {
            if(filterHelper==null)
            {
                filterHelper=new FilterHelper(currentList,this,c);
            }

            return filterHelper;
        }
        public void refresh(){
            notifyDataSetChanged();
        }
    }

    /*
    Our HTTP Client
     */
    public class JSONDownloader {

        //SAVE/RETRIEVE URLS
        private static final String JSON_DATA_URL="https://cdn.rawgit.com/Oclemy/SampleJSON/338d9585/spacecrafts.json";
        //INSTANCE FIELDS
        private final Context c;

        public JSONDownloader(Context c) {
            this.c = c;
        }
        /*
        Fetch JSON Data
         */
        public ArrayList<Spacecraft> retrieve(final ListView mListView, final ProgressBar myProgressBar)
        {
            final ArrayList<Spacecraft> downloadedData=new ArrayList<>();
            myProgressBar.setIndeterminate(true);
            myProgressBar.setVisibility(View.VISIBLE);

            AndroidNetworking.get(JSON_DATA_URL)
                    .setPriority(Priority.HIGH)
                    .build()
                    .getAsJSONArray(new JSONArrayRequestListener() {
                        @Override
                        public void onResponse(JSONArray response) {
                            JSONObject jo;
                            Spacecraft s;
                            try
                            {
                                for(int i=0;i<response.length();i++)
                                {
                                    jo=response.getJSONObject(i);

                                    int id=jo.getInt("id");
                                    String name=jo.getString("name");
                                    String propellant=jo.getString("propellant");
                                    String techExists=jo.getString("technologyexists");
                                    String imageURL=jo.getString("imageurl");

                                    s=new Spacecraft();
                                    s.setId(id);
                                    s.setName(name);
                                    s.setPropellant(propellant);
                                    s.setImageURL(imageURL);
                                    s.setTechnologyExists(techExists.equalsIgnoreCase("1") ? 1 : 0);

                                    downloadedData.add(s);
                                }
                                myProgressBar.setVisibility(View.GONE);

                            }catch (JSONException e)
                            {
                                myProgressBar.setVisibility(View.GONE);
                                Toast.makeText(c, "GOOD RESPONSE BUT JAVA CAN'T PARSE JSON IT RECEIEVED. "+e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                        //ERROR
                        @Override
                        public void onError(ANError anError) {
                            anError.printStackTrace();
                            myProgressBar.setVisibility(View.GONE);
                            Toast.makeText(c, "UNSUCCESSFUL :  ERROR IS : "+anError.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
            return downloadedData;
        }
    }
    ArrayList<Spacecraft> spacecrafts = new ArrayList<>();
    ListView myListView;
    ListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myListView= findViewById(R.id.myListView);
        final ProgressBar myProgressBar= findViewById(R.id.myProgressBar);
        SearchView mySearchView=findViewById(R.id.mySearchView);

        mySearchView.setIconified(true);
        mySearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        mySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                adapter.getFilter().filter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                adapter.getFilter().filter(query);
                return false;
            }
        });
        spacecrafts=new JSONDownloader(MainActivity.this).retrieve(myListView,myProgressBar);
        adapter=new ListViewAdapter(this,spacecrafts);
        myListView.setAdapter(adapter);

    }
}