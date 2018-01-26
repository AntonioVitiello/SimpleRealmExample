package io.realm.examples;

/**
 * Created by antlap on 27/10/2017.
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.examples.model.Cat;
import io.realm.examples.model.Dog;
import io.realm.examples.model.Person;

public class MainActivity extends Activity {
    private final boolean clearDatabaseFirst = true;

    public static final String TAG = MainActivity.class.getName();
    private LinearLayout rootLayout = null;

    private Realm realm;

    private Random mRandomIntGenerator = new Random();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rootLayout = ((LinearLayout) findViewById(R.id.container));
        rootLayout.removeAllViews();

        // These operations are small enough that
        // we can generally safely run them on the UI thread.

        // Create the Realm instance
        realm = Realm.getDefaultInstance();

        clearDatabase();
        basicCRUD(realm);
        basicQuery(realm);
        basicLinkQuery(realm);

        // More complex operations can be executed on another thread.
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String info;
                info = complexReadWrite();
                info += complexQuery();
                return info;
            }

            @Override
            protected void onPostExecute(String result) {
                String[] status = result.split("<end>");
                showTitle("Complex Read/Write operation...");
                showStatus(status[0]);
                showTitle("Complex Query operation...");
                showStatus(status[1]);
            }
        }.execute();

    }

    private void clearDatabase(){
        showTitle("Delete half database operation...");
        showStatus("Realm database contains " + realm.where(Person.class).count() + " persons");
        if(clearDatabaseFirst) {
            // Delete persons
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<Person> all = realm.where(Person.class).findAll();
                    int halfPersonCount = all.size() / 2;
                    for(int i = 0; i < halfPersonCount; i++){
                        all.deleteFromRealm(getRandomInt(0, all.size()));
                    }
/*
                    Delete all Person from database
                    realm.delete(Person.class);
*/
                    showStatus("[DELETE]\nNow Realm database contains "
                            + realm.where(Person.class).count() + " persons");
                }
            });
        }
    }

    /* <View
            android:layout_width="match_parent"
            android:layout_height="2dp"
            android:layout_marginTop="1dp"
            android:layout_marginBottom="1dp"
            android:background="@color/colorPrimary" />

            <View style="@style/Divider"/>  */
    private View getLineSeparator() {
/* Metodo 1:
        View lineView = new View(this);
        lineView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(WRAP_CONTENT, 8);
        params.topMargin = 16;
        params.bottomMargin = 4;
        lineView.setLayoutParams(params);
        return lineView;
*/

/* Metodo 2:
        View lineView;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            lineView = new View(this, null, 0, R.style.Divider);
        }
        return lineView;
*/

/* Metodo 3:
see: https://mobikul.com/create-custom-attributes-reference-color-style-xml-android/
see: http://www.sherif.mobi/2012/12/applying-style-to-views-dynamically-in.html
 */
        return new View(this, null, R.attr.lineDivider);
    }

    private void showTitle(String txt) {
        Log.i(TAG, "showTitle: " + txt);

        TextView tv = new TextView(this);
        tv.setText(txt);
//        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
//        tv.setLayoutParams(params);

        rootLayout.addView(getLineSeparator());
        rootLayout.addView(tv);
        rootLayout.addView(getLineSeparator());
    }

    private void showStatus(String txt) {
        Log.i(TAG, txt);
        TextView tv = new TextView(this);
        tv.setText(txt);
        rootLayout.addView(tv);
    }

    private void basicCRUD(Realm realm) {
        showTitle("Basic CRUD operations...");

        // Count Persons in databasa
        long count = realm.where(Person.class).count();
        showStatus("Realm database contains " + count + " persons");

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                // Add a person
                Person person = realm.createObject(Person.class);
                person.setId(1);
                person.setName("Young Person");
                person.setAge(34);
                person.setDog(realm.createObject(Dog.class));
                person.getDog().name = "Carlino";
                Cat cat1 = realm.createObject(Cat.class);
                cat1.name = "Vincent";
                Cat cat2 = realm.createObject(Cat.class);
                cat2.name = "Theo";
                person.setCats(new RealmList<Cat>(cat1, cat2));
                showStatus("[CREATE]\n" + person);
            }
        });

        // Find the first person (no query conditions) and read a field
        final Person person = realm.where(Person.class).equalTo("age", 34).findFirst();
        showStatus("[READ]\nThe first Person where age=34: " + person.getName() + ", age = " + person.getAge());

        // Update person in a transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                person.setName("Old Person");
                person.setAge(66);
                showStatus("[UPDATE]\n" + person.getName() + " got older: " + person.getAge());
            }
        });

        // Delete a person
        showStatus("Before deleting a Person, Realm database contains " + realm.where(Person.class).count() + " persons");
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                String personInfo = person.getName() + ", age = " + person.getAge();
                person.deleteFromRealm();
                showStatus("[DELETE]\nDeleted a Person: " + personInfo);
            }
        });
        showStatus("After deleting a Person, Realm database contains " + realm.where(Person.class).count() + " persons");

    }

    private void basicQuery(Realm realm) {
        showTitle("Basic Query operation...");
        showStatus("Number of persons: " + realm.where(Person.class).count());

        int minAge = 20;
        int maxAge = 50;
        RealmResults<Person> results = realm.where(Person.class)
                .beginGroup()
                .contains("name", " no. ")
                .between("age", 20, 50)
                .equalTo("dog.name", "fido")
//                .beginsWith("cat.name", "Cat")
                .endGroup()
                .findAll();

        showStatus("[READ]\nNumber of Persons with age between " + minAge + " and " + maxAge + ": " + results.size());
        showStatus(results.toString());
    }

    private void basicLinkQuery(Realm realm) {
        showTitle("Basic Link Query operation...");

        // Add a Person with a cat
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Person person = realm.createObject(Person.class);
//                Person person = realm.where(Person.class).findFirst();
                person.setName("Antonio");
                person.setAge(20);
                person.setDog(realm.createObject(Dog.class));
                person.getDog().name = "Tornaacasa Lessi";
                Cat cat = realm.createObject(Cat.class);
                cat.name = "Mice Tiger Woods";
                person.getCats().add(cat);
            }
        });

        showStatus("Number of persons in database: " + realm.where(Person.class).count());

//        RealmResults<Person> results = realm.where(Person.class).equalTo("cats.name", "Mice Tiger Woods").findAll();
        RealmResults<Person> results = realm.where(Person.class)
                .beginGroup()
                .contains("cats.name", "Tiger", Case.INSENSITIVE)
                .endGroup()
                .findAll();

        showStatus("Number of Persons with a cat like Tiger: " + results.size());
        if(results.size() > 0) {
            showStatus(results.toString());
        }
    }

    private int getRandomInt(int min, int max){
        return mRandomIntGenerator.nextInt(max) + min;
    }

    private String complexReadWrite() {
        String status = "Number of persons: ";

        // Open the default realm. All threads must use its own reference to the realm.
        // Those can not be transferred across threads.
        Realm realm = Realm.getDefaultInstance();

        // Add ten persons in one transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Dog fido = realm.createObject(Dog.class);
                fido.name = "fido";
                for (int i = 0; i < 10; i++) {
                    Person person = realm.createObject(Person.class);
                    person.setId(i);
                    person.setName("Person no. " + i);
                    person.setAge(getRandomInt(i, i * 10 + 1));
                    person.setDog(fido);

                    // The field tempReference is annotated with @Ignore.
                    // This means setTempReference sets the Person tempReference
                    // field directly. The tempReference is NOT saved as part of
                    // the RealmObject:
                    person.setTempReference(42);

                    for (int j = 0; j < i; j++) {
                        Cat cat = realm.createObject(Cat.class);
                        cat.name = "Cat_" + j;
                        person.getCats().add(cat);
                    }
                }
            }
        });

        // Implicit read transactions allow you to access your objects
        status += realm.where(Person.class).count();

        // Iterate over all objects
        for (Person pers : realm.where(Person.class).findAll()) {
            String dogName;
            if (pers.getDog() == null) {
                dogName = "None";
            } else {
                dogName = pers.getDog().name;
            }
            status += "\n" + pers.getName() + ":" + pers.getAge() + " : " + dogName + " : " + pers.getCats().size();
        }

        // Sorting
        RealmResults<Person> sortedPersons = realm.where(Person.class).findAllSorted("age", Sort.DESCENDING);
        status += "\nSorting " + sortedPersons.last().getName() + " == " + realm.where(Person.class).findFirst()
                .getName();

        realm.close();
        return status + "<end>";
    }

    private String complexQuery() {
        String status = "Number of persons: ";

        Realm realm = Realm.getDefaultInstance();
        status += realm.where(Person.class).count();

        // Find all persons where age between 20 and 50 and name begins with "Person".
        RealmResults<Person> results = realm.where(Person.class)
                .between("age", 20, 50)       // Notice implicit "and" operation
                .beginsWith("name", "Person").findAll();
        status += "\nSize of result set: " + results.size();

        realm.close();

        return status + "<end>";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close(); // Remember to close Realm when done.
    }

}