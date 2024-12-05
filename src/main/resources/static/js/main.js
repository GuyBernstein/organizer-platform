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

  // Initialize tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })

  // Initialize copy buttons
  document.querySelectorAll('.copy-button').forEach(button => {
    button.addEventListener('click', function() {
      const content = this.getAttribute('data-content');
      navigator.clipboard.writeText(content).then(() => {
        // Create or update tooltip
        const tooltip = bootstrap.Tooltip.getInstance(this);
        const originalTitle = this.getAttribute('data-bs-original-title');

        // Update tooltip content
        tooltip.setContent({ '.tooltip-inner': 'הועתק!' });

        // Show the updated tooltip
        tooltip.show();

        // Reset tooltip after delay
        setTimeout(() => {
          tooltip.setContent({ '.tooltip-inner': originalTitle });
        }, 1500);
      }).catch(err => {
        console.error('Failed to copy text: ', err);
      });
    });
  });
});

// Keep track of loaded messages per subcategory
const loadedMessages = {}

function loadMoreMessages(categoryKey, subcategoryKey) {
  const key = `${categoryKey}-${subcategoryKey}`
  const offset = (loadedMessages[key] || 0) + 50

  // Make AJAX call to load more messages
  fetch(`/api/messages?category=${categoryKey}&subcategory=${subcategoryKey}&offset=${offset}&limit=50`)
    .then(response => response.json())
    .then(data => {
      loadedMessages[key] = offset
      // Append new messages to the container
      const container = document.querySelector(`[data-category="${categoryKey}"][data-subcategory="${subcategoryKey}"] .messages-container`)
      // Append new message buttons...
    })
}

// intersection observer for infinite scrolling
const observerOptions = {
  root: null,
  rootMargin: '20px',
  threshold: 0.1
}

const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const container = entry.target
      const categoryKey = container.dataset.category
      const subcategoryKey = container.dataset.subcategory
      loadMoreMessages(categoryKey, subcategoryKey)
    }
  })
}, observerOptions)

// Observe all message containers
document.querySelectorAll('.messages-container').forEach(container => {
  observer.observe(container)
})
// toggle between view and edit mode in the modal
function toggleEditMode(button) {
  const modal = button.closest('.modal');
  const viewMode = modal.querySelector('.view-mode');
  const editMode = modal.querySelector('.edit-mode');
  const editButton = modal.querySelector('.edit-text');

  if (viewMode.classList.contains('d-none')) {
    // Switch to View Mode
    viewMode.classList.remove('d-none');
    editMode.classList.add('d-none');
    editButton.textContent = 'ערוך';
  } else {
    // Switch to Edit Mode
    viewMode.classList.add('d-none');
    editMode.classList.remove('d-none');
    editButton.textContent = 'תצוגה';
  }
}

