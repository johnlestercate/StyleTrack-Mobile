package com.styletrack.customer.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.styletrack.customer.R;
import com.styletrack.customer.data.Appointment;
import com.styletrack.customer.data.PayOptions;
import com.styletrack.customer.data.RepositoryCore;
import com.styletrack.customer.ui.common.Appointments;
import com.styletrack.customer.ui.common.FactRow;
import com.styletrack.customer.ui.common.Formats;
import com.styletrack.customer.ui.common.ScreenHeader;
import com.styletrack.customer.ui.common.StatusChip;
import com.styletrack.customer.ui.common.UiState;

import java.util.concurrent.CompletionException;

/** Replaces BookingDetailScreen in BookingsScreen.kt. */
public class BookingDetailFragment extends Fragment implements PaymentDialog.Listener {

    private BookingDetailViewModel vm;
    private NavController nav;
    private long appointmentId;
    private boolean busy;
    private boolean onlinePaymentOn;
    private Appointment current;

    private ProgressBar progress;
    private View content;
    private TextView serviceName, status, statusMessage, error;
    private View paySection;
    private MaterialButton payGcash, payQrph, changeButton, cancelButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_booking_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        appointmentId = getArguments() != null ? getArguments().getLong("appointmentId") : -1L;
        nav = NavHostFragment.findNavController(this);
        ScreenHeader.bind(view, "Booking", nav::navigateUp);

        vm = new ViewModelProvider(this, new BookingDetailViewModel.Factory(requireActivity().getApplication(), appointmentId))
                .get(BookingDetailViewModel.class);

        progress = view.findViewById(R.id.progress);
        content = view.findViewById(R.id.content);
        serviceName = view.findViewById(R.id.serviceName);
        status = view.findViewById(R.id.status);
        statusMessage = view.findViewById(R.id.statusMessage);
        error = view.findViewById(R.id.error);
        paySection = view.findViewById(R.id.paySection);
        payGcash = view.findViewById(R.id.payGcash);
        payQrph = view.findViewById(R.id.payQrph);
        changeButton = view.findViewById(R.id.changeButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        payGcash.setOnClickListener(v -> startPayment("GCASH"));
        payQrph.setOnClickListener(v -> startPayment("QRPH"));
        changeButton.setOnClickListener(v -> nav.navigate(R.id.reschedule, AppNav.idArg("appointmentId", appointmentId)));
        cancelButton.setOnClickListener(v -> confirmCancel());

        vm.payOptions.state().observe(getViewLifecycleOwner(), state -> {
            onlinePaymentOn = state instanceof UiState.Ready && ((UiState.Ready<PayOptions>) state).data.enabled;
            if (current != null) render(current);
        });
        vm.appointment.state().observe(getViewLifecycleOwner(), state -> {
            progress.setVisibility(state instanceof UiState.Loading ? View.VISIBLE : View.GONE);
            if (state instanceof UiState.Ready) {
                content.setVisibility(View.VISIBLE);
                current = ((UiState.Ready<Appointment>) state).data;
                render(current);
                maybeShowPaymentDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) vm.appointment.refreshIfStale();
    }

    private void maybeShowPaymentDialog() {
        if (vm.payment != null && getChildFragmentManager().findFragmentByTag("payment") == null) {
            PaymentDialog.newInstance(appointmentId, vm.payment).show(getChildFragmentManager(), "payment");
        }
    }

    private void render(Appointment a) {
        serviceName.setText(a.serviceName);
        StatusChip.bind(status, a.status);

        statusMessage.setVisibility(View.GONE);
        switch (a.status) {
            case "PENDING":
                statusMessage.setText("Waiting for the salon to confirm. We'll notify you as soon as they do.");
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant));
                statusMessage.setVisibility(View.VISIBLE);
                break;
            case "ACCEPTED":
                statusMessage.setText("Your booking is confirmed. See you then!");
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                statusMessage.setVisibility(View.VISIBLE);
                break;
            case "REJECTED":
                String reason = (a.rejectionReason != null && !a.rejectionReason.isBlank()) ? ": " + a.rejectionReason : ".";
                statusMessage.setText("The salon couldn't take this booking" + reason + " You're welcome to book another time.");
                statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
                statusMessage.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

        View root = requireView();
        FactRow.bind(root.findViewById(R.id.rowDate), "Date", Formats.fmtDay(a.startTime));
        FactRow.bind(root.findViewById(R.id.rowTime), "Time", Formats.fmtTime(a.startTime) + " – " + Formats.fmtTime(a.endTime));
        FactRow.bind(root.findViewById(R.id.rowStylist), "Stylist", a.stylistName);
        FactRow.bind(root.findViewById(R.id.rowType), "Type", a.isHomeService() ? "Home visit" : "At the salon");
        if (a.isHomeService() && a.address != null && !a.address.isBlank()) {
            FactRow.bind(root.findViewById(R.id.rowAddress), "Address", a.address);
        } else {
            FactRow.hide(root.findViewById(R.id.rowAddress));
        }
        if (a.notes != null && !a.notes.isBlank()) {
            FactRow.bind(root.findViewById(R.id.rowNotes), "Your notes", a.notes);
        } else {
            FactRow.hide(root.findViewById(R.id.rowNotes));
        }

        FactRow.bind(root.findViewById(R.id.rowService), "Service", Formats.peso(a.totalAmount - a.homeServiceFee));
        if (a.isHomeService()) {
            FactRow.bind(root.findViewById(R.id.rowHomeFee), "Home visit fee", Formats.peso(a.homeServiceFee));
        } else {
            FactRow.hide(root.findViewById(R.id.rowHomeFee));
        }
        FactRow.bind(root.findViewById(R.id.rowTotal), "Total", Formats.peso(a.totalAmount));
        String paymentText;
        if ("PAID".equals(a.paymentStatus)) {
            paymentText = "Paid" + (a.paymentMethod != null ? " (" + methodLabel(a.paymentMethod) + ")" : "");
        } else {
            paymentText = onlinePaymentOn ? "Not paid yet" : "Pay at the salon";
        }
        FactRow.bind(root.findViewById(R.id.rowPayment), "Payment", paymentText);
        if ("COMPLETED".equals(a.status) && a.pointsEarned > 0) {
            FactRow.bind(root.findViewById(R.id.rowPoints), "Points earned", String.valueOf(a.pointsEarned));
        } else {
            FactRow.hide(root.findViewById(R.id.rowPoints));
        }

        boolean payable = onlinePaymentOn && !"PAID".equals(a.paymentStatus)
                && ("ACCEPTED".equals(a.status) || "COMPLETED".equals(a.status));
        paySection.setVisibility(payable ? View.VISIBLE : View.GONE);
        payGcash.setEnabled(!busy);
        payQrph.setEnabled(!busy);

        boolean canChange = Appointments.canChange(a);
        changeButton.setVisibility(canChange ? View.VISIBLE : View.GONE);
        cancelButton.setVisibility(canChange ? View.VISIBLE : View.GONE);
        changeButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        cancelButton.setText(busy ? "Please wait…" : "Cancel booking");
    }

    private static String methodLabel(String method) {
        switch (method) {
            case "GCASH": return "GCash";
            case "QRPH": return "QR Ph";
            case "CASH": return "cash";
            default: return method;
        }
    }

    private void startPayment(String method) {
        busy = true;
        error.setVisibility(View.GONE);
        if (current != null) render(current);
        vm.startPayment(appointmentId, method).whenCompleteAsync((payStarted, err) -> {
            busy = false;
            if (!isAdded()) return;
            if (err == null) {
                vm.payment = payStarted;
                maybeShowPaymentDialog();
            } else {
                error.setText(RepositoryCore.userMessage(unwrap(err)));
                error.setVisibility(View.VISIBLE);
            }
            if (current != null) render(current);
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onPaymentFinished(boolean paid) {
        vm.payment = null;
        vm.appointment.reload(true);
        if (paid) Snackbar.make(requireView(), "Payment received. Thank you!", Snackbar.LENGTH_LONG).show();
    }

    private void confirmCancel() {
        if (current == null) return;
        Appointment a = current;
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel this booking?")
                .setMessage(a.serviceName + " on " + Formats.fmtWhen(a.startTime) + " will be cancelled.")
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Cancel booking", (d, w) -> doCancel(a.id))
                .show();
    }

    private void doCancel(long id) {
        busy = true;
        error.setVisibility(View.GONE);
        if (current != null) render(current);
        vm.cancel(id).whenCompleteAsync((a, err) -> {
            busy = false;
            if (!isAdded()) return;
            if (err == null) {
                Snackbar.make(requireView(), "Booking cancelled.", Snackbar.LENGTH_LONG).show();
                vm.appointment.reload(true);
            } else {
                error.setText(RepositoryCore.userMessage(unwrap(err)));
                error.setVisibility(View.VISIBLE);
            }
            if (current != null) render(current);
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private static Throwable unwrap(Throwable t) {
        return (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
    }
}
