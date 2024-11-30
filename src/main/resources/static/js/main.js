document.addEventListener('DOMContentLoaded', function() {
  const alerts = document.querySelectorAll('.alert')
  alerts.forEach(function(alert) {
    if (!alert.classList.contains('alert-permanent')) {
      setTimeout(function() {
        alert.classList.add('fade');
        setTimeout(function() {
          alert.remove();
        }, 150);
      }, 5000);
    }
  });
});