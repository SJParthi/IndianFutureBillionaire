// File: src/main/resources/static/js/hpc-dashboard.js
/********************************************************************************
 * HPC meltdown synergy front-end JavaScript.
 *  1) On page load => parse concurrencyStatsJson => build a Chart.js line chart
 *  2) Possibly do meltdown blinking or refresh intervals
 ********************************************************************************/

document.addEventListener("DOMContentLoaded", function() {

  // meltdown aggregator concurrency chart
  let chartCanvas = document.getElementById("concurrencyChart");
  if(chartCanvas && typeof concurrencyStatsJson !== "undefined" && concurrencyStatsJson) {
    let ctx = chartCanvas.getContext("2d");
    let data = {
      labels: concurrencyStatsJson.labels || [],
      datasets: [
        {
          label: "Partial Bar Expansions",
          data: concurrencyStatsJson.partialBarCount || [],
          borderColor: "rgb(75,192,192)",
          fill: false,
          tension: 0
        },
        {
          label: "Meltdown Triggers",
          data: concurrencyStatsJson.meltdownTriggers || [],
          borderColor: "rgb(255,99,132)",
          fill: false,
          tension: 0
        }
      ]
    };
    let chartOptions = {
      scales: {
        y: { beginAtZero: true }
      }
    };
    let concurrencyChart = new Chart(ctx, {
      type: 'line',
      data: data,
      options: chartOptions
    });
  }

  console.log("HPC meltdown synergy front-end loaded");
});
