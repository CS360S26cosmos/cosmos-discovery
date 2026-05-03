package com.example.cosmos_discovery.database;

import com.example.cosmos_discovery.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminServiceTest {

    /**
     * updateUserRole with "admin" as newRole must now succeed (reach Firestore),
     * not be rejected by a client-side guard.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void updateUserRole_adminNewRole_reachesFirestore() {
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDoc = mock(DocumentReference.class);
        Task<Void> mockTask = mock(Task.class);

        when(mockDb.collection("users")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDoc);
        when(mockDoc.update(anyMap())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        AdminService service = new AdminService(mockDb);

        service.updateUserRole("someUserId", User.ROLE_ADMIN, () -> {}, err -> {});

        // Verify Firestore was actually called (not short-circuited)
        verify(mockDoc).update(anyMap());
    }

    /**
     * updateUserRole with "admin" sets role to "admin" in the update map.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void updateUserRole_adminNewRole_setsCorrectRoleInUpdate() {
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        DocumentReference mockDoc = mock(DocumentReference.class);
        Task<Void> mockTask = mock(Task.class);

        when(mockDb.collection("users")).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDoc);
        when(mockDoc.update(anyMap())).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);
        when(mockTask.addOnFailureListener(any())).thenReturn(mockTask);

        AdminService service = new AdminService(mockDb);

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        service.updateUserRole("someUserId", User.ROLE_ADMIN, () -> {}, err -> {});

        verify(mockDoc).update(mapCaptor.capture());
        assertEquals(User.ROLE_ADMIN, mapCaptor.getValue().get("role"));
    }
}
